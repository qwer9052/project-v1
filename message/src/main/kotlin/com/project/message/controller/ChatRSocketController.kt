package com.project.message.controller

import com.project.message.dto.ChatMessageDTO
import com.project.message.service.ChatKafkaService
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux

@Controller
class ChatRSocketController(
    private val kafkaService: ChatKafkaService
) {
    init {
        println("[RSocket Controller] 🚀 ChatRSocketController 초기화됨")
    }

    @MessageMapping("room.{roomId}")
    fun chatChannel(
        @DestinationVariable roomId: String,
        inbound: Flux<ChatMessageDTO>
    ): Flux<ChatMessageDTO> {
        println("[RSocket] 📥 채널 오픈: room=$roomId")

        // 1️⃣ DB에서 최근 메시지 로드 (시간차 두고 전송)
        val recentMessages = kafkaService.loadRecentMessages(roomId, 50)
        val recent = if (recentMessages.isNotEmpty()) {
            println("[RSocket] 📚 최근 메시지 ${recentMessages.size}개 로드")
            Flux.fromIterable(recentMessages)
                .delaySequence(java.time.Duration.ofMillis(500))
                .doOnNext { msg ->
                    println("[RSocket] ✅ 최근 메시지 클라이언트 전송: ${msg.message}")
                }
        } else {
            println("[RSocket] 📭 최근 메시지 없음")
            Flux.empty()
        }
            .onErrorResume { e ->
                println("[RSocket] ❌ 최근 메시지 로드 실패: ${e.message}")
                Flux.empty()
            }

        // 2️⃣ 클라이언트에서 받은 메시지 처리 → Kafka 발행
        val processed = inbound
            .doOnNext { msg ->
                println("[RSocket] 📨 클라이언트 메시지 수신: room=$roomId, sender=${msg.senderId}, message=${msg.message}")
                // Kafka에 발행 (다른 클라이언트들도 받도록)
                kafkaService.publishMessage(msg.copy(roomId = roomId))
            }
            .onErrorResume { e ->
                println("[RSocket] ⚠️ 인바운드 에러: ${e.message}")
                Flux.empty()
            }
            .doOnComplete {
                println("[RSocket] ✅ 인바운드 완료")
            }

        // 3️⃣ Kafka 스트림 구독 (다른 클라이언트가 보낸 메시지 + 자신이 보낸 메시지도 포함)
        val outbound = kafkaService.getRoomFlux(roomId)
            .doOnNext { msg ->
                println("[RSocket] 📤 Kafka 메시지 클라이언트에 전송: sender=${msg.senderId}, message=${msg.message}")
            }
            .onErrorResume { e ->
                println("[RSocket] ❌ Kafka 아웃바운드 에러: ${e.message}")
                Flux.empty()
            }
            .doOnCancel {
                println("[RSocket] 🔌 Kafka 구독 취소됨: room=$roomId")
            }

        // 4️⃣ 최근 메시지 + Kafka 스트림 결합
        // (클라이언트 인바운드는 processed에서 처리하고, outbound로만 클라이언트에 전송)
        return recent
            .concatWith(outbound)
            .doOnCancel {
                println("[RSocket] 🔌 채널 취소됨: room=$roomId")
                kafkaService.cleanupRoom(roomId)
            }
            .doOnComplete {
                println("[RSocket] ✅ 채널 완료: room=$roomId")
                kafkaService.cleanupRoom(roomId)
            }
            .doOnError { e ->
                println("[RSocket] ❌ 채널 에러: ${e.message}")
                e.printStackTrace()
                kafkaService.cleanupRoom(roomId)
            }
            // processed를 병렬로 구독 (클라이언트 메시지 처리용, 클라이언트에는 안 보냄)
            .mergeWith(
                processed
                    .ignoreElements()
                    .flux()
            )
    }
}