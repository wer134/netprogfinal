import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class RegisterServer {
    private static final int PORT = 8888;
    private static Map<String, String> registry = new HashMap<>();

    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(PORT);
        byte[] buffer = new byte[1024];

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            String msg = new String(packet.getData(), 0, packet.getLength());

            String response = handle(msg.trim());
            byte[] responseData = response.getBytes();
            DatagramPacket responsePacket = new DatagramPacket(
                responseData, responseData.length, packet.getAddress(), packet.getPort());
            socket.send(responsePacket);
        }
    }

    private static String handle(String msg) {
        String[] parts = msg.split(" ");
        switch (parts[0]) {
            case "REGISTER":
                registry.put(parts[1], parts[2] + ":" + parts[3]);
                return "OK";
            case "QUERY":
                return registry.containsKey(parts[1])
                        ? "FOUND " + registry.get(parts[1])
                        : "NOT_FOUND";
            default:
                return "ERROR Unknown Command";
        }
    }
}

