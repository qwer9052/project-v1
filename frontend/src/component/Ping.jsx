// Ping.jsx (0.x 라인 최소 예제)
import React, {useEffect, useState} from "react";
import {encodeAndAddWellKnownMetadata, encodeRoute, JsonSerializers, RSocketClient,} from "rsocket-core";
import RSocketWebSocketClient from "rsocket-websocket-client";

const ROUTING_MIME_ID = 0x7e;     // routing
const COMPOSITE_MIME_ID = 0x7f;   // composite metadata

function routingMetadata(route) {
    const encoded = encodeRoute(route);
    return encodeAndAddWellKnownMetadata(
        Buffer.alloc(0),
        ROUTING_MIME_ID,
        encoded
    );
}

export default function Ping() {
    const [socket, setSocket] = useState(null);
    const [last, setLast] = useState("");

    useEffect(() => {
        const transport = new (RSocketWebSocketClient.default || RSocketWebSocketClient)({
            url: "ws://localhost:8081/rsocket"
        });

        const client = new RSocketClient({
            setup: {
                keepAlive: 60000,
                lifetime: 180000,
                dataMimeType: "application/json",
                metadataMimeType: COMPOSITE_MIME_ID
            },
            serializers: JsonSerializers, // ✅ 0.x
            transport: transport
        });

        const sub = client.connect().subscribe({
            onComplete: setSocket,
            onError: console.error,
        });

        return () => sub?.cancel?.();
    }, []);

    const sendPing = () => {
        if (!socket) return;
        socket.requestResponse({
            data: {msg: "ping"},
            metadata: routingMetadata("ping"),
        }).subscribe({
            onNext: (payload) => setLast(JSON.stringify(payload.data)),
            onError: console.error,
            onComplete: () => {
                console.log("ping");
            },
            onSubscribe: () => {
                console.log("ping");
            }
        });
    };

    return (
        <div style={{padding: 16}}>
            <button onClick={sendPing} disabled={!socket}>PING</button>
            <div style={{marginTop: 8}}>last: {last}</div>
        </div>
    );
}