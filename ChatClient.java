import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    public static void main(String[] args) throws Exception {
        String myId = "user2";
        String myIp = InetAddress.getLocalHost().getHostAddress();
        int myPort = 9001;

        // 1. Register to RegisterServer
        DatagramSocket udpSocket = new DatagramSocket();
        String registerMsg = "REGISTER " + myId + " " + myIp + " " + myPort;
        byte[] data = registerMsg.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("localhost"), 8888);
        udpSocket.send(packet);

        // 2. Query for another user
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter target ID: ");
        String targetId = sc.nextLine();
        String query = "QUERY " + targetId;
        udpSocket.send(new DatagramPacket(query.getBytes(), query.length(), packet.getAddress(), 8888));

        DatagramPacket response = new DatagramPacket(new byte[1024], 1024);
        udpSocket.receive(response);
        String resp = new String(response.getData(), 0, response.getLength());

        if (!resp.startsWith("FOUND")) {
            System.out.println("User not found");
            return;
        }

        String[] ipPort = resp.split(" ")[1].split(":");
        Socket tcpSocket = new Socket(ipPort[0], Integer.parseInt(ipPort[1]));

        // 3. Start chatting
        new Thread(() -> {
            try (Scanner in = new Scanner(tcpSocket.getInputStream())) {
                while (in.hasNextLine())
                    System.out.println("[RECV] " + in.nextLine());
            } catch (IOException e) {}
        }).start();

        PrintWriter out = new PrintWriter(tcpSocket.getOutputStream(), true);
        while (true) {
            String line = sc.nextLine();
            out.println(line);
        }
    }
}

