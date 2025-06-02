public class UserInfo {
    private String userId;
    private String ipAddress;
    private int port;
    private String protocol; // "TCP" 또는 "UDP"
    private long timestamp;  // 등록 시각 (옵션)

    public UserInfo(String userId, String ipAddress, int port, String protocol) {
        this.userId = userId;
        this.ipAddress = ipAddress;
        this.port = port;
        this.protocol = protocol;
        this.timestamp = System.currentTimeMillis(); // 현재 시간
    }

    public String getUserId() {
        return userId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public String getProtocol() {
        return protocol;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void update(String ipAddress, int port, String protocol) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.protocol = protocol;
        this.timestamp = System.currentTimeMillis(); // 갱신 시간 업데이트
    }

    @Override
    public String toString() {
        return String.format("UserID: %s, IP: %s, Port: %d, Protocol: %s, Time: %d",
                userId, ipAddress, port, protocol, timestamp);
    }

    public String toResponseString() {
        return ipAddress + ":" + port;
    }
}

