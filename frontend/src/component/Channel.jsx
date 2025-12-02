
// 채널 컴포넌트 (선택된 채널 표시)
const Channel = ({ channel }) => {
    return (
        <div style={{ padding: "10px", borderBottom: "1px solid #ddd" }}>
            <h3>{channel ? `#${channel.name}` : "Select a channel"}</h3>
        </div>
    );
};


export default Channel