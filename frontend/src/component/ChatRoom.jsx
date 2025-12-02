import React, {useState} from "react";
import "@chatscope/chat-ui-kit-styles/dist/default/styles.min.css";
import {ChatContainer, MainContainer, Message, MessageInput, MessageList,} from "@chatscope/chat-ui-kit-react";
import {useChatRoom} from "./useChatRoom";

export default function ChatRoom({ roomId, senderId }) {
    const {messages, send} = useChatRoom(roomId, senderId);
    const [text, setText] = useState("");

    const handleSend = () => {
        const trimmed = text.trim();
        if (!trimmed) return;
        send(trimmed);
        setText("");
    };

    return (
        <div style={{position: "relative", height: "500px"}}>
            <MainContainer>
                <ChatContainer>
                    <MessageList>
                        {messages.length === 0 ? (
                            <Message model={{message: "아직 메시지가 없습니다.", direction: "incoming", sender: "System"}}/>
                        ) : (
                            messages.map((m, idx) => (
                                <Message
                                    key={idx}
                                    model={{
                                        message: m.message, // ✅ 서버 DTO 필드명에 맞춤
                                        direction: m.senderId === senderId ? "outgoing" : "incoming",
                                        sender: m.senderId,
                                        sentTime: new Date(m.timestamp || Date.now()).toLocaleTimeString(),
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