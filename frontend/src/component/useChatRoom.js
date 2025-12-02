import {useCallback, useEffect, useRef, useState} from "react";
import {
    encodeCompositeMetadata,
    encodeRoute,
    JsonSerializers,
    MESSAGE_RSOCKET_COMPOSITE_METADATA,
    MESSAGE_RSOCKET_ROUTING,
    RSocketClient,
} from "rsocket-core";
import RSocketWebSocketClient from "rsocket-websocket-client";

const JSON_MIME = "application/json";
const COMPOSITE_MIME = MESSAGE_RSOCKET_COMPOSITE_METADATA.string;

function createRoutingMetadata(route) {
    const routeBuffer = encodeRoute(route);
    return encodeCompositeMetadata([
        [MESSAGE_RSOCKET_ROUTING, routeBuffer]
    ]);
}

export function useChatRoom(roomId, senderId, { url = "ws://localhost:8081/rsocket" } = {}) {
    const [messages, setMessages] = useState([]);
    const socketRef = useRef(null);
    const channelSubscriberRef = useRef(null);

    useEffect(() => {
        const TransportCtor = RSocketWebSocketClient.default || RSocketWebSocketClient;
        const transport = new TransportCtor({ url });

        const client = new RSocketClient({
            setup: {
                keepAlive: 10000,
                lifetime: 180000,
                dataMimeType: JSON_MIME,
                metadataMimeType: COMPOSITE_MIME,
            },
            serializers: JsonSerializers,
            transport,
        });

        const sub = client.connect().subscribe({
            onComplete: (socket) => {
                socketRef.current = socket;
                console.log("[RSocket] Connected");

                const route = `chat.${roomId}`;
                console.log(`[RSocket] Starting channel for: ${route}`);

                const metadata = createRoutingMetadata(route);
                let isFirstMessage = true;

                const publisher = {
                    subscribe: (subscriber) => {
                        console.log("[Publisher] Server subscribed");

                        channelSubscriberRef.current = subscriber;

                        subscriber.onSubscribe({
                            request: (n) => {
                                console.log(`[Publisher] Server requested ${n}`);

                                // 서버가 request하면 즉시 첫 메시지 전송 (메타데이터 포함)
                                if (isFirstMessage && n > 0) {
                                    isFirstMessage = false;
                                    console.log("[Publisher] Sending first message with metadata");

                                    subscriber.onNext({
                                        data: {
                                            roomId,
                                            senderId,
                                            message: "[INIT]",
                                            timestamp: new Date().toISOString()
                                        },
                                        metadata: metadata  // ✅ 첫 메시지에 메타데이터 포함
                                    });
                                }
                            },
                            cancel: () => {
                                console.log("[Publisher] Cancelled");
                                channelSubscriberRef.current = null;
                            },
                        });
                    }
                };

                console.log("[RSocket] Calling requestChannel");

                // requestChannel 호출 - publisher만 전달
                const channel = socket.requestChannel(publisher);

                channel.subscribe({
                    onSubscribe: (subscription) => {
                        console.log("[Channel] Subscribed to responses");
                        subscription.request(2147483647);
                    },
                    onNext: (payload) => {
                        console.log("[Channel] ✅ Received:", payload.data);
                        if (payload?.data) {
                            setMessages((prev) => [...prev, payload.data]);
                        }
                    },
                    onError: (err) => {
                        console.error("[Channel] ❌ Error:", err);
                        if (err.source) {
                            console.error("[Channel] Error source:", err.source);
                        }
                    },
                    onComplete: () => {
                        console.log("[Channel] Completed");
                    },
                });
            },
            onError: (err) => {
                console.error("[RSocket] Connection error:", err);
            },
        });

        return () => {
            console.log("[RSocket] Cleanup");
            if (channelSubscriberRef.current) {
                try {
                    channelSubscriberRef.current.onComplete();
                } catch (e) {}
            }
            channelSubscriberRef.current = null;
            sub.cancel();
            try {
                socketRef.current?.close();
            } catch (e) {}
            socketRef.current = null;
        };
    }, [url, roomId, senderId]);

    const send = useCallback(
        (text) => {
            if (!channelSubscriberRef.current) {
                console.error("[Send] Channel not ready");
                return;
            }

            console.log("[Send] Sending:", text);

            try {
                channelSubscriberRef.current.onNext({
                    data: {
                        roomId,
                        senderId,
                        message: text,
                        timestamp: new Date().toISOString()
                    }
                    // 이후 메시지는 metadata 없음
                });
                console.log("[Send] ✅ Sent");
            } catch (err) {
                console.error("[Send] ❌ Error:", err);
            }
        },
        [roomId, senderId]
    );

    return { messages, send };
}