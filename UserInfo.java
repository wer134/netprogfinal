import java.io.Serializable;

public class UserInfo implements Serializable {
    private String username;
    private String ip;
    private int port;

    public UserInfo(String username, String ip, int port) {
        this.username = username;
        this.ip = ip;
        this.port = port;
    }

    public String getUsername() { return username; }
    public String getIp() { return ip; }
    public int getPort() { return port; }

    @Override
    public String toString() {
        return username + "@" + ip + ":" + port;
    }
}

