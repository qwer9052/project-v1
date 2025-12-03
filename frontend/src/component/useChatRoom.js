import {useCallback, useEffect, useRef, useState} from "react";
import {
    BufferEncoders,
    encodeCompositeMetadata,
    encodeRoute,
    MESSAGE_RSOCKET_COMPOSITE_METADATA,
    MESSAGE_RSOCKET_ROUTING,
    RSocketClient,
} from "rsocket-core";
import RSocketWebSocketClient from "rsocket-websocket-client";

/** 직렬화기: Buffer/문자열 그대로 통과 */
const IdentitySerializer = {
    serialize: (data) => data,
    deserialize: (data) => data,
};

/** 객체 → JSON → Buffer */
const toJsonBuffer = (obj) => Buffer.from(JSON.stringify(obj), "utf8");

/** Uint8Array → Buffer */
const toBuffer = (u8) => Buffer.from(u8);

export function useChatRoom(
    roomId,
    senderId,
    { url = "ws://localhost:8081/rsocket" } = {}
) {
    const [messages, setMessages] = useState([]);
    const socketRef = useRef(null);
    const channelSubscriberRef = useRef(null);

    const route = `room.${roomId}`;
    // 라우팅 메타데이터
    const routeMetadataU8 = encodeCompositeMetadata([
        [MESSAGE_RSOCKET_ROUTING, encodeRoute(route)],
    ]);
    const routeMetadataBuf = toBuffer(routeMetadataU8);

    useEffect(() => {
        console.log(`[Client] 🔌 ${url}에 연결 중...`);

        const TransportCtor = RSocketWebSocketClient?.default || RSocketWebSocketClient;
        const transport = new TransportCtor({ url }, BufferEncoders);

        const client = new RSocketClient({
            setup: {
                keepAlive: 100000,
                lifetime: 180000,
                dataMimeType: "application/json",
                metadataMimeType: MESSAGE_RSOCKET_COMPOSITE_METADATA.string,
            },
            serializers: {
                data: IdentitySerializer,
                metadata: IdentitySerializer,
            },
            transport,
        });

        const sub = client.connect().subscribe({
            onComplete: (socket) => {
                socketRef.current = socket;
                console.log("[Client] ✅ RSocket 연결됨");

                // 🔹 Publisher: 클라이언트 → 서버 메시지 스트림
                const publisher = {
                    subscribe: (subscriber) => {
                        console.log("[Client] 📤 Publisher 구독됨");
                        channelSubscriberRef.current = subscriber;

                        subscriber.onSubscribe({
                            request: (n) => {
                                console.log(`[Client] 📥 서버가 ${n}개 요청함`);
                            },
                            cancel: () => {
                                console.log("[Client] ❌ 채널 취소됨");
                                channelSubscriberRef.current = null;
                            },
                        });

                        // 초기 메시지: 라우팅 메타데이터 포함
                        console.log(`[Client] 🚀 초기 메시지 전송: room=${roomId}`);
                        subscriber.onNext({
                            metadata: routeMetadataBuf,
                            data: toJsonBuffer({
                                roomId: roomId,
                                senderId,
                                message: senderId,
                                timestamp: new Date().toISOString(),
                            }),
                        });
                    },
                };

                // 🔹 채널 오픈: requestChannel으로 양방향 통신 시작
                console.log(`[Client] 📡 채널 오픈: room=${roomId}`);
                const channel = socket.requestChannel(publisher);

                channel.subscribe({
                    onSubscribe: (subscription) => {
                        // console.log("[Client] ✅ 채널 구독 완료");
                        // 서버에서 무제한으로 메시지 요청
                        subscription.request(2147483647);
                    },

                    onNext: (payload) => {
                        // console.log("[Client] 📨 메시지 수신:", payload);
                        const u8 = payload?.data;
                        if (u8 == null) {
                            console.warn("[Client] ⚠️ data 없음");
                            return;
                        }

                        try {
                            const text =
                                typeof u8 === "string"
                                    ? u8
                                    : new TextDecoder().decode(u8);
                            const msg =
                                typeof text === "string"
                                    ? JSON.parse(text)
                                    : text;

                            // console.log("[Client] ✅ 파싱 완료:", msg);
                            setMessages((prev) => [...prev, msg]);
                        } catch (e) {
                            console.warn("[Client] ❌ 파싱 실패:", e);
                            setMessages((prev) => [...prev, { raw: u8 }]);
                        }
                    },

                    onError: (err) => {
                        console.error("[Client] ❌ 채널 오류:", err);
                    },

                    onComplete: () => {
                        console.log("[Client] 🔌 채널 종료");
                    },
                });
            },

            onError: (err) => {
                console.error("[Client] ❌ 연결 오류:", err);
            },
        });

        return () => {
            console.log("[Client] 🧹 정리 중...");
            try {
                channelSubscriberRef.current?.onComplete();
            } catch (_) {}
            channelSubscriberRef.current = null;

            try {
                sub.cancel();
            } catch (_) {}

            try {
                socketRef.current?.close();
            } catch (_) {}
            socketRef.current = null;
        };
    }, [url, roomId, senderId]);

    const send = useCallback(
        (text) => {
            if (!channelSubscriberRef.current) {
                console.error("[Client] ❌ 채널이 준비되지 않음");
                return;
            }

            console.log("[Client] 📨 메시지 전송:", text);

            try {
                channelSubscriberRef.current.onNext({
                    metadata: routeMetadataBuf,
                    data: toJsonBuffer({
                        roomId,
                        senderId,
                        message: text,
                        timestamp: new Date().toISOString(),
                    }),
                });
                console.log("[Client] ✅ 전송 완료");
            } catch (err) {
                console.error("[Client] ❌ 전송 오류:", err);
            }
        },
        [roomId, senderId, routeMetadataBuf]
    );

    return { messages, send };
}