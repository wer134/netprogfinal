import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;

public class RegisterServer {
    private static final int PORT = 9000;
    private static final Map<String, UserInfo> userMap = Collections.synchronizedMap(new LinkedHashMap<>());

    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(PORT);
        System.out.println("[RegisterServer] UDP 등록 서버 시작 (포트: " + PORT + ")");

        byte[] buffer = new byte[1024];

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            String msg = new String(packet.getData(), 0, packet.getLength()).trim();
            String[] parts = msg.split(" ");
            String cmd = parts[0];

            if (cmd.equals("REGISTER") && parts.length == 4) {
                String username = parts[1];
                String ip = parts[2];
                int port = Integer.parseInt(parts[3]);
                if (userMap.containsKey(username)) {
                    sendResponse(socket, packet, "FAIL 이미 등록된 이름입니다.");
                } else {
                    userMap.put(username, new UserInfo(username, ip, port));
                    sendResponse(socket, packet, "OK");
                    System.out.println("[RegisterServer] 등록: " + username + " (" + ip + ":" + port + ")");
                }
            } else if (cmd.equals("LIST")) {
                StringBuilder sb = new StringBuilder();
                for (String name : userMap.keySet()) {
                    sb.append(name).append(" ");
                }
                sendResponse(socket, packet, sb.toString().trim());
            } else {
                sendResponse(socket, packet, "FAIL 명령어 오류");
            }
        }
    }

    private static void sendResponse(DatagramSocket socket, DatagramPacket packet, String msg) throws Exception {
        byte[] data = msg.getBytes();
        DatagramPacket resp = new DatagramPacket(
                data, data.length, packet.getAddress(), packet.getPort());
        socket.send(resp);
    }
}

