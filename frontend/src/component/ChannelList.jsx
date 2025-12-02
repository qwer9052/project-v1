// 채널 리스트 컴포넌트
const ChannelList = ({ channels, onSelectChannel }) => {
    return (
        <div style={{ width: "200px", backgroundColor: "#f4f4f4", padding: "10px" }}>
            <h4 style={{color : '#000'}}>Channels</h4>
            <ul style={{ listStyle: "none", padding: 0 }}>
                {channels.map((channel) => (
                    <li
                        key={channel.id}
                        style={{ padding: "8px", cursor: "pointer", color : '#000' }}
                        onClick={() => onSelectChannel(channel)}
                    >
                        #{channel.name}
                    </li>
                ))}
            </ul>
        </div>
    );
};
export default ChannelList;