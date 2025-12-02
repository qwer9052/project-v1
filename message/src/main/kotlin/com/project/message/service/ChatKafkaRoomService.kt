package com.project.message.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.project.message.dto.ChatMessageDTO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max


@Service
class ChatKafkaRoomService(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val consumerFactory: ConsumerFactory<String, String>,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val objectMapper = jacksonObjectMapper()

    private val topic = "chat-messages"
    private val rooms = ConcurrentHashMap<String, MutableSharedFlow<ChatMessageDTO>>()
    private val scope = CoroutineScope(Dispatchers.Default)

    fun roomFlow(roomId: String): MutableSharedFlow<ChatMessageDTO> =
        rooms.computeIfAbsent(roomId) {
            MutableSharedFlow(
                replay = 0,
                extraBufferCapacity = 1024,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
        }

    /** 실시간 송신 (클라 -> 서버 -> Kafka) */
    fun publish(msg: ChatMessageDTO) {
        val payload = objectMapper.writeValueAsString(msg)
        kafkaTemplate.send(topic, msg.roomId, payload)
    }

    /** 실시간 수신 (Kafka -> 서버 -> 각 클라) */
    @KafkaListener(topics = ["chat-messages"])
    fun consume(
        payload: String,
        @Header(KafkaHeaders.RECEIVED_KEY) key: String?,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topicName: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
    ) {
        try {
            val msg = objectMapper.readValue(payload, ChatMessageDTO::class.java)
            scope.launch { roomFlow(msg.roomId).emit(msg) }
        } catch (e: Exception) {
            log.warn("Invalid payload on $topicName-$partition: $payload", e)
        }
    }

    /**
     * 과거 N개 메시지 빠른 로드:
     * - roomId를 key로 보냈으므로 같은 파티션에 모여 순서 보장.
     * - 끝 오프셋(end) 기준으로 역방향으로 구간을 스캔하며 roomId 매칭만 수집.
     * - 매우 큰 토픽이라면 DB 병행 저장으로 더 빠르게 처리 가능.
     */
    suspend fun loadRecent(roomId: String, count: Int, maxScan: Long = 10_000L): List<ChatMessageDTO> =
        withContext(Dispatchers.IO) {
            val consumer: Consumer<String, String> = consumerFactory.createConsumer()
            try {
                val partitionsInfo = consumer.partitionsFor(topic)
                val numPartitions = partitionsInfo.size
                val partitionId = kafkaPartitionForKey(roomId, numPartitions)
                val tp = TopicPartition(topic, partitionId)

                consumer.assign(listOf(tp))

                val end = consumer.endOffsets(listOf(tp))[tp] ?: 0L
                if (end == 0L) return@withContext emptyList()

                val step = maxScan.coerceAtLeast(count.toLong() * 5)
                var start = max(0L, end - step)
                val acc = ArrayDeque<ChatMessageDTO>()

                while (acc.size < count && start >= 0) {
                    consumer.seek(tp, start)
                    val polled = consumer.poll(Duration.ofMillis(500))
                    val batch = polled.records(tp)
                        .asSequence()
                        .filter { it.key() == roomId }
                        .map { objectMapper.readValue(it.value(), ChatMessageDTO::class.java) }
                        .toList()

                    acc.addAll(batch.takeLast(count - acc.size))
                    if (acc.size >= count || start == 0L) break
                    val nextStart = max(0L, start - step)
                    if (nextStart == start) break
                    start = nextStart
                }

                acc.takeLast(count).toList()
            } finally {
                runCatching { consumer.close(Duration.ofSeconds(1)) }
            }
        }

    /** Kafka DefaultPartitioner와 동일한 murmur2 해시 기반 파티션 계산 */
    private fun kafkaPartitionForKey(key: String, numPartitions: Int): Int {
        val hash = murmur2(key.toByteArray(Charsets.UTF_8))
        return (hash and 0x7FFFFFFF) % numPartitions
    }


    private fun murmur2(data: ByteArray): Int {
        val seed = 0x9747b28c.toInt()
        var length = data.size
        var h = seed
        var i = 0

        while (length >= 4) {
            var k = (data[i].toInt() and 0xFF) or
                    ((data[i + 1].toInt() and 0xFF) shl 8) or
                    ((data[i + 2].toInt() and 0xFF) shl 16) or
                    ((data[i + 3].toInt() and 0xFF) shl 24)

            k *= 0x5bd1e995
            k = k xor (k ushr 24)
            k *= 0x5bd1e995.toInt()

            h *= 0x5bd1e995.toInt()
            h = h xor k

            i += 4
            length -= 4
        }

        when (length) {
            3 -> {
                h = h xor ((data[i + 2].toInt() and 0xFF) shl 16)
                h = h xor ((data[i + 1].toInt() and 0xFF) shl 8)
                h = h xor (data[i].toInt() and 0xFF)
                h *= 0x5bd1e995.toInt()
            }

            2 -> {
                h = h xor ((data[i + 1].toInt() and 0xFF) shl 8)
                h = h xor (data[i].toInt() and 0xFF)
                h *= 0x5bd1e995.toInt()
            }

            1 -> {
                h = h xor (data[i].toInt() and 0xFF)
                h *= 0x5bd1e995.toInt()
            }

            0 -> {
                // nothing
            }
        }

        h = h xor (h ushr 13)
        h *= 0x5bd1e995.toInt()
        h = h xor (h ushr 15)

        return h

    }
}