import React, {useEffect, useState} from "react";
import keycloak from "./component/keycloak";
import '@chatscope/chat-ui-kit-styles/dist/default/styles.min.css';
import {ChatContainer, MainContainer, Message, MessageInput, MessageList} from "@chatscope/chat-ui-kit-react";

const App = () => {
    const [authenticated, setAuthenticated] = useState(false);

    useEffect(() => {
        keycloak.init({ onLoad: "check-sso" }).then(auth => {
            setAuthenticated(auth);
            if (!auth) {
                // 로그인 유도: 자동 리다이렉트
                keycloak.login();
            }
        });
    }, []);

    if (!authenticated) {
        return <div style={{ textAlign: "center", marginTop: "50px" }}>
            <h2>로그인 중...</h2>
        </div>;
    }

    return (
        <div style={{ position: "relative", height: "100vh" }}>
            <MainContainer>
                <ChatContainer>
                    <MessageList>
                        <Message model={{
                            message: "Keycloak 로그인 성공! 채팅 화면입니다.",
                            sentTime: "just now",
                            sender: "System"
                        }} />
                    </MessageList>
                    <MessageInput placeholder="메시지를 입력하세요..." />
                </ChatContainer>
            </MainContainer>
        </div>
    );
};

export default App
