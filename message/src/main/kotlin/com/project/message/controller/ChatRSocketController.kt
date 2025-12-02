import com.project.message.dto.ChatMessageDTO
import com.project.message.service.ChatKafkaRoomService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.annotation.ConnectMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Mono

@Controller
class ChatRSocketController(
    private val roomService: ChatKafkaRoomService
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        println("[RSocket Controller] 🚀 ChatRSocketController initialized")
    }

    @MessageMapping("chat.{roomId}")
    fun chatChannel(
        @DestinationVariable roomId: String?,
        inbound: Flow<ChatMessageDTO>
    ): Flow<ChatMessageDTO> = channelFlow {
        println("[RSocket] 🔥🔥🔥 CHANNEL OPEN: room=$roomId, thread=${Thread.currentThread().name}")

        // 1) 접속 직후 최근 N개 먼저 전송
        scope.launch {
            println("[RSocket] 📥 Loading recent messages for room=$roomId")
            try {
                roomId?.let { roomId ->
                    val recent = roomService.loadRecent(roomId, count = 50)
                    println("[RSocket] 📦 Loaded ${recent.size} recent messages")
                    recent.forEach {
                        println("[RSocket] 📤 Sending recent: $it")
                        send(it)
                    }
                    println("[RSocket] ✅ All recent messages sent")
                }

            } catch (e: Exception) {
                println("[RSocket] ❌ Error loading recent: ${e.message}")
                e.printStackTrace()
            }
        }

        // 2) 클라 → 서버로 들어오는 채널 메시지 → Kafka publish
        val inboundJob = scope.launch {
            println("[RSocket] 🎧 Starting inbound message collection for room=$roomId")
            try {
                var count = 0
                inbound.collect { msg ->
                    count++
                    println("[RSocket SERVER] ✅✅✅ RECEIVED message #$count: $msg")
                    roomId?.let { roomId ->
                        roomService.publish(msg.copy(roomId = roomId))
                    }
                }
            } catch (e: Exception) {
                println("[RSocket] ❌ Inbound collection error: ${e.message}")
                e.printStackTrace()
            }
        }

        // 3) Kafka 소비 → SharedFlow를 통해 실시간 fan-out
        val outboundJob = scope.launch {
            println("[RSocket] 📡 Starting outbound Kafka flow for room=$roomId")
            try {
                roomId?.let { roomId ->
                    roomService.roomFlow(roomId).collect {
                        println("[RSocket] 📤 Sending outbound from Kafka: $it")
                        send(it)
                    }
                }

            } catch (e: Exception) {
                println("[RSocket] ❌ Outbound flow error: ${e.message}")
                e.printStackTrace()
            }
        }

        awaitClose {
            println("[RSocket] 🔌 CHANNEL CLOSE: room=$roomId")
            inboundJob.cancel()
            outboundJob.cancel()
        }
    }

    @ConnectMapping
    fun onConnect(requester: RSocketRequester): Mono<Void> {
        println("[RSocket] 🤝 CONNECT attempt: requester=$requester")
        val remote = requester.rsocket()
        return (remote?.onClose()
            ?.doFirst { println("[RSocket] ✅ CONNECTED: requester=$requester") }
            ?.doOnError { e -> println("[RSocket] ❌ CLOSED with error: ${e.message}") }
            ?.doFinally { println("[RSocket] 👋 CLOSED: requester=$requester") }
            ?.then()) ?: Mono.empty()
    }
}