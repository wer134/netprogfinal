import java.util.Date; // Date 클래스 임포트

public class UserInfo {
    private String id;
    private String ip;
    private int port;
    private long lastActiveTime; // 마지막 활동 시간 (밀리초 단위)

    public UserInfo(String id, String ip, int port) {
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.lastActiveTime = System.currentTimeMillis(); // 생성 시 현재 시간으로 초기화
    }

    public String getId() { return id; }
    public String getIp() { return ip; }
    public int getPort() { return port; }

    // lastActiveTime에 대한 getter 및 setter 추가
    public long getLastActiveTime() { return lastActiveTime; }
    public void setLastActiveTime(long lastActiveTime) { this.lastActiveTime = lastActiveTime; }

    @Override
    public String toString() {
        return "ID: " + id + ", IP: " + ip + ", Port: " + port + ", Last Active: " + new Date(lastActiveTime);
    }
}
