import java.net.*;
import java.util.*;

public class RegisterServer {
    private static final int PORT = 9000;
    private static Map<String, UserInfo> userMap = new HashMap<>();

    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(PORT);
        System.out.println("RegisterServer(UDP) 대기중...");

        while (true) {
            byte[] buf = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);

            String msg = new String(packet.getData(), 0, packet.getLength());
            String response;

            if (msg.equals("/list")) {
                StringBuilder sb = new StringBuilder();
                synchronized (userMap) {
                    for (UserInfo info : userMap.values())
                        sb.append(info.toString()).append("\n");
                }
                response = sb.length() > 0 ? sb.toString().trim() : "No users.";
            } else {
                String[] split = msg.split(":");
                if (split.length == 3) {
                    String username = split[0], ip = split[1];
                    int port = Integer.parseInt(split[2]);
                    boolean portExists = false, nameExists = false;
                    synchronized (userMap) {
                        for (UserInfo info : userMap.values()) {
                            if (info.getPort() == port) portExists = true;
                            if (info.getUsername().equals(username)) nameExists = true;
                        }
                        if (nameExists || portExists) {
                            response = "FAIL:이미 등록됨";
                        } else {
                            UserInfo info = new UserInfo(username, ip, port);
                            userMap.put(username, info);
                            response = "OK";
                            System.out.println("[REGISTER] " + info);
                        }
                    }
                } else {
                    response = "FAIL:형식오류";
                }
            }
            socket.send(new DatagramPacket(response.getBytes(), response.length(), packet.getAddress(), packet.getPort()));
        }
    }
}

