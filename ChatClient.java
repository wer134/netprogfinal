import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        // UDP로 등록
        DatagramSocket udp = new DatagramSocket();
        System.out.print("이름 입력: ");
        String username = sc.nextLine().trim();
        System.out.print("포트번호 입력(예: 10123): ");
        int port = Integer.parseInt(sc.nextLine().trim());

        // 서버 정보
        String regServerIp = "localhost";
        int regServerPort = 9000;

        // UDP 등록 요청
        String regMsg = "REGISTER " + username + " " + InetAddress.getLocalHost().getHostAddress() + " " + port;
        byte[] data = regMsg.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(regServerIp), regServerPort);
        udp.send(packet);

        // 응답 수신
        byte[] buf = new byte[1024];
        DatagramPacket resp = new DatagramPacket(buf, buf.length);
        udp.receive(resp);
        String respMsg = new String(resp.getData(), 0, resp.getLength());
        System.out.println("등록 응답: " + respMsg);
        if (!respMsg.startsWith("OK")) return;

        // TCP 채팅 서버 연결
        Socket socket = new Socket("localhost", port);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        // 이름 전송
        out.println(username);

        // 서버 응답 및 송/수신 스레드
        Thread recv = new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    System.out.println(msg);
                }
            } catch (IOException e) {}
        });
        recv.setDaemon(true);
        recv.start();

        // 입력 루프
        while (true) {
            String input = sc.nextLine();
            out.println(input);
            if (input.equalsIgnoreCase("/quit")) break;
        }
        socket.close();
        udp.close();
    }
}

