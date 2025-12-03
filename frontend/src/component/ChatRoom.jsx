import React, {useEffect, useState} from "react";
import "@chatscope/chat-ui-kit-styles/dist/default/styles.min.css";
import {ChatContainer, MainContainer, Message, MessageInput, MessageList,} from "@chatscope/chat-ui-kit-react";
import {useChatRoom} from "./useChatRoom";

export default function ChatRoom({ roomId, senderId }) {
    const { messages, send } = useChatRoom(roomId, senderId);
    const [text, setText] = useState("");

    useEffect(() => {
        // console.log("[ChatRoom] 📊 메시지 업데이트:", messages.length, "개", messages);
    }, [messages]);

    const handleSend = () => {
        const trimmed = text.trim();
        if (!trimmed) return;
        console.log("[ChatRoom] 🚀 메시지 전송:", trimmed);
        send(trimmed);
        setText("");
    };

    // 메시지가 유효한지 확인
    const isValidMessage = (msg) => {
        return msg && typeof msg === 'object' && msg.message && msg.senderId && msg.timestamp;
    };

    // 필터링된 메시지만 표시
    const validMessages = messages.filter(isValidMessage);

    console.log("[ChatRoom] 렌더링 - 총 메시지:", messages.length, "유효한 메시지:", validMessages.length);

    return (
        <div style={{ position: "relative", height: "500px", width: "100%" }}>
            <MainContainer>
                <ChatContainer>
                    <MessageList>
                        {validMessages.length === 0 ? (
                            <Message
                                model={{
                                    message: "아직 메시지가 없습니다.",
                                    direction: "incoming",
                                    sender: "System",
                                }}
                            />
                        ) : (
                            validMessages.map((m, idx) => (
                                <Message
                                    key={idx}
                                    model={{
                                        message: m.senderId + " : " +m.message || "메시지 없음",
                                        direction: m.senderId === senderId ? "outgoing" : "incoming",
                                        sender: m.senderId || "Unknown",
                                        sentTime: m.timestamp
                                            ? new Date(m.timestamp).toLocaleTimeString("ko-KR")
                                            : "시간 없음",
                                    }}
                                />
                            ))
                        )}
                    </MessageList>

                    <MessageInput
                        placeholder="메시지를 입력하세요"
                        value={text}
                        onChange={(val) => setText(val)}
                        onSend={handleSend}
                        attachButton={false}
                    />
                </ChatContainer>
            </MainContainer>
        </div>
    );
}