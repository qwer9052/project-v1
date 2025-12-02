import React, {useEffect, useState} from "react";
import keycloak from "./component/keycloak";
import '@chatscope/chat-ui-kit-styles/dist/default/styles.min.css';
import {MainContainer} from "@chatscope/chat-ui-kit-react";
import ChannelList from "./component/ChannelList.jsx";
import Channel from "./component/Channel.jsx";
import axios from 'axios';
import ChatRoom from "./component/ChatRoom.jsx";

const App = () => {
    const [authenticated, setAuthenticated] = useState(false);

    const [selectedChannel, setSelectedChannel] = useState([]);

    useEffect(() => {
        keycloak.init({ onLoad: "check-sso" }).then(auth => {
            setAuthenticated(auth);
            if (!auth) {
                // 로그인 유도: 자동 리다이렉트
                keycloak.login();
            }
        });
    }, []);


    // 2) 토큰 자동 갱신 (선택)
    useEffect(() => {
        if (!authenticated) return;

        const intervalId = setInterval(() => {
            keycloak
                .updateToken(30) // 남은 시간 30초 미만이면 갱신 시도
                .catch((err) => console.error("Token refresh failed:", err));
        }, 20_000);

        keycloak.onTokenExpired = () => {
            keycloak
                .updateToken(60)
                .catch((err) => console.error("Token refresh on expire failed:", err));
        };

        return () => clearInterval(intervalId);
    }, [authenticated]);


    // 3) 인증 완료 후에만 API 호출 (한 번만/필요 시 재호출 제어)
    useEffect(() => {
        if (!authenticated) return;

        const source = axios.CancelToken.source();

        const commonConfig = {
            withCredentials: true,
            headers: {
                Authorization: `Bearer ${keycloak.token}`,
                // 'Content-Type': 'application/json', // GET이면 보통 불필요
            },
            cancelToken: source.token,
        };

        // me 호출
        axios
            .get("http://localhost:8081/me", commonConfig)
            .then((res) => {
                console.log("me:", res.data);
            })
            .catch((err) => {
                if (!axios.isCancel(err)) {
                    console.error("GET /me error:", err);
                }
            });

        // channels 호출
        axios
            .get("http://localhost:8081/channels", commonConfig)
            .then((res) => {
                if (res.status === 200) {
                    setSelectedChannel(res.data); // ✅ then 내부에서 상태 갱신
                }
                console.log("channels:", res.data);
            })
            .catch((err) => {
                if (!axios.isCancel(err)) {
                    console.error("GET /channels error:", err);
                }
            });

        return () => {
            source.cancel("component unmounted");
        };
    }, [authenticated]); // ✅ 의존성: authenticated


    if (!authenticated) {
        return <div style={{ textAlign: "center", marginTop: "50px" }}>
            <h2>로그인 중...</h2>
        </div>;
    }

    return (
        <div style={{ display:"flex", height: "100vh", width: "100vh" }}>
            <ChannelList channels={selectedChannel} onSelectChannel={setSelectedChannel} />
            <MainContainer style={{width:'100vw', display:"flex", flexDirection : 'column'}}>
                <Channel style={{color : '#000'}} channel={selectedChannel} />
                <ChatRoom roomId="roomA" senderId="namju" />
                {/*<ChatContainer>*/}
                {/*    <MessageList>*/}
                {/*        <Message model={{*/}
                {/*            message: "Keycloak 로그인 asf성sdfsf공! 채팅 화면입니다.",*/}
                {/*            sentTime: "just nosdfsfw",*/}
                {/*            sender: "Systemdfdfdf"*/}
                {/*        }} />*/}
                {/*    </MessageList>*/}
                {/*    <MessageInput placeholder="메시지를 입력하세요..." />*/}
                {/*</ChatContainer>*/}
            </MainContainer>
        </div>
    );
};

export default App
