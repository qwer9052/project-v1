import React, {useEffect, useState} from "react";
import keycloak from "./component/keycloak";
import '@chatscope/chat-ui-kit-styles/dist/default/styles.min.css';
import {ChatContainer, MainContainer, Message, MessageInput, MessageList} from "@chatscope/chat-ui-kit-react";
import ChannelList from "./component/ChannelList.jsx";
import Channel from "./component/Channel.jsx";

const App = () => {
    const [authenticated, setAuthenticated] = useState(false);

    const [selectedChannel, setSelectedChannel] = React.useState(null);
    const channels = [
        { id: 1, name: "general" },
        { id: 2, name: "random" },
        { id: 3, name: "dev" }
    ];


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
        <div style={{ display:"flex", height: "100vh", width: "100vh" }}>
            <ChannelList channels={channels} onSelectChannel={setSelectedChannel} />
            <MainContainer style={{width:'100vw', display:"flex", flexDirection : 'column'}}>
                <Channel style={{color : '#000'}} channel={selectedChannel} />
                <ChatContainer>
                    <MessageList>
                        <Message model={{
                            message: "Keycloak 로그인 asf성sdfsf공! 채팅 화면입니다.",
                            sentTime: "just nosdfsfw",
                            sender: "Systemdfdfdf"
                        }} />
                    </MessageList>
                    <MessageInput placeholder="메시지를 입력하세요..." />
                </ChatContainer>
            </MainContainer>
        </div>
    );
};

export default App
