
package com.project.message.service

import com.project.message.dto.ChatMessageDTO
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.time.Duration
import java.util.*

@Service
class ChatKafkaService(
    private val kafkaTemplate: KafkaTemplate<String, ChatMessageDTO>,
    @Value("\${spring.kafka.bootstrap-servers:localhost:9092}")
    private val bootstrapServers: String
) {
    /**
     * 🔊 글로벌 브로드캐스트 Sink (방별 맵 제거)
     * - 제한 버퍼로 메모리 폭주 방지
     * - 모든 방 메시지를 한 곳으로 모아 스트림 제공, 클라이언트는 roomId로 필터
     */
    private val broadcastSink: Sinks.Many<ChatMessageDTO> = Sinks.many()
        .multicast()
        .onBackpressureBuffer(
            /* bufferSize */ 10_000,
            /* overflowStrategy */ false
        )

    private val broadcastFlux: Flux<ChatMessageDTO> = broadcastSink.asFlux()

    init {
        println("[ChatKafkaService] 🚀 초기화됨")
    }

    /**
     * 특정 채팅방의 라이브 메시지 스트림 제공
     * - 글로벌 스트림에서 roomId로 필터
     */
    fun getRoomFlux(roomId: String): Flux<ChatMessageDTO> {
        println("[Kafka] 📡 룸 플럭스 조회: room=$roomId")
        return broadcastFlux
            .filter { it.roomId == roomId }
            .doOnSubscribe { println("[Kafka] 👥 구독 시작: room=$roomId") }
            .doFinally { signal -> println("[Kafka] 👥 구독 종료: room=$roomId, signal=$signal") }
    }

    /**
     * 채팅방에 메시지 발행 (Kafka에 발행)
     * - 낮은 레이턴시를 원하면 로컬 브로드캐스트에도 함께 발행
     */
    fun publishMessage(msg: ChatMessageDTO) {
        println("[Kafka] 📤 메시지 발행: room=${msg.roomId}, message=${msg.message}")

        // Kafka 발행 (키에 roomId 설정 → 파티션 단위 순서 보장)
        kafkaTemplate.send("chat-messages", msg.roomId, msg)
            .whenComplete { result, ex ->
                if (ex != null) {
                    println("[Kafka] ❌ Kafka 발행 실패: ${ex.message}")
                } else {
                    println("[Kafka] ✅ Kafka 발행 완료: partition=${result.recordMetadata.partition()}, offset=${result.recordMetadata.offset()}")
                }
            }

        // 선택: 로컬 브로드캐스트로 즉시 전달(지연 최소화)
        val emit = broadcastSink.tryEmitNext(msg)
        if (emit.isFailure) {
            println("[Kafka] ⚠️ 로컬 브로드캐스트 실패: room=${msg.roomId}, result=$emit")
        }
    }

    /**
     * Kafka에서 최근 메시지 로드 (정확한 재생)
     * - 각 파티션의 endOffsets에서 limit만큼 뒤로 이동하여 읽기
     * - roomId 키로 필터링 후 마지막 limit개 반환
     */
    fun loadRecentMessages(roomId: String, limit: Int): List<ChatMessageDTO> {
        println("[Kafka] 📚 최근 메시지 로드 시작: room=$roomId, limit=$limit")

        return try {
            val props = Properties().apply {
                put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
                put(ConsumerConfig.GROUP_ID_CONFIG, "history-reader-${System.currentTimeMillis()}")
                put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer::class.java.name)
                put(JsonDeserializer.VALUE_DEFAULT_TYPE, ChatMessageDTO::class.java.name)
                put(JsonDeserializer.TRUSTED_PACKAGES, "*")
                put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
                put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000")
            }

            val messages = mutableListOf<ChatMessageDTO>()
            KafkaConsumer<String, ChatMessageDTO>(props).use { consumer ->
                val topic = "chat-messages"

                // 파티션 정보 조회 후 직접 assign
                val partitionsInfo = consumer.partitionsFor(topic) ?: emptyList()
                if (partitionsInfo.isEmpty()) {
                    println("[Kafka] ⚠️ 파티션 정보 없음")
                    return emptyList()
                }
                val topicPartitions = partitionsInfo.map { TopicPartition(topic, it.partition()) }
                consumer.assign(topicPartitions)

                // 시작/끝 오프셋 조회
                val beginningOffsets = consumer.beginningOffsets(topicPartitions)
                val endOffsets = consumer.endOffsets(topicPartitions)

                // 각 파티션의 읽기 시작점 계산: max(begin, end - limit)
                topicPartitions.forEach { tp ->
                    val begin = beginningOffsets[tp] ?: 0L
                    val end = endOffsets[tp] ?: begin
                    val start = kotlin.math.max(begin, end - limit.toLong())
                    consumer.seek(tp, start)
                    println("[Kafka] 🔎 Partition=${tp.partition()}, begin=$begin, end=$end, start=$start")
                }

                // 읽기 루프
                val pollTimeout = Duration.ofSeconds(2)
                val collected = mutableListOf<ChatMessageDTO>()
                var consecutiveEmpty = 0

                while (consecutiveEmpty < 3) {
                    val records = consumer.poll(pollTimeout)
                    if (records.isEmpty) {
                        consecutiveEmpty++
                    } else {
                        consecutiveEmpty = 0
                        records.forEach { record ->
                            if (record.key() == roomId && record.value() != null) {
                                collected.add(record.value())
                            }
                        }
                        // 충분히 모였으면 일찍 종료
                        if (collected.size >= limit) break
                    }
                }

                messages.addAll(collected.takeLast(limit))
                println("[Kafka] ✅ ${messages.size}개 메시지 로드 완료")
            }

            messages
        } catch (e: Exception) {
            println("[Kafka] ❌ 최근 메시지 로드 실패: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Kafka에서 메시지 수신 → 글로벌 브로드캐스트로 전파
     */
    @KafkaListener(topics = ["chat-messages"], groupId = "chat-consumer")
    fun consumeMessage(msg: ChatMessageDTO) {
        println("[Kafka] 🔔 Kafka에서 메시지 수신: room=${msg.roomId}, message=${msg.message}")

        val result = broadcastSink.tryEmitNext(msg)
        if (result.isFailure) {
            println("[Kafka] ⚠️ 브로드캐스트 실패: room=${msg.roomId}, result=$result")
        } else {
            println("[Kafka] ✅ 브로드캐스트 성공")
        }
    }

    /**
     * 방 정리 (글로벌 Sink 방식에서는 별도 리소스 없음)
     * - 필요 시 noop 또는 향후 room별 자원 사용 시 구현
     */
    fun cleanupRoom(roomId: String) {
        println("[Kafka] 🧹 채팅방 정리(글로벌 Sink 사용): room=$roomId")
        // No-op
    }
}
