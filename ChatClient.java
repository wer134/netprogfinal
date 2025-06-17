import java.io.*;
import java.net.*;
import java.util.*;

public class ChatClient {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        // 1. UDP 등록
        System.out.print("이름 입력: ");
        String username = sc.nextLine().trim();
        System.out.print("포트번호 입력(예: 10123): ");
        int port = Integer.parseInt(sc.nextLine().trim());
        String ip = InetAddress.getLocalHost().getHostAddress();

        DatagramSocket udp = new DatagramSocket();
        String regMsg = username + ":" + ip + ":" + port;
        DatagramPacket regPack = new DatagramPacket(
                regMsg.getBytes(), regMsg.length(),
                InetAddress.getByName("localhost"), 9000);
        udp.send(regPack);

        byte[] buf = new byte[1000];
        DatagramPacket recvPack = new DatagramPacket(buf, buf.length);
        udp.receive(recvPack);
        String regResult = new String(buf, 0, recvPack.getLength());
        System.out.println("등록 응답: " + regResult);
        udp.close();
        if (!regResult.startsWith("OK")) {
            System.out.println("등록 실패! 종료."); return;
        }

        // 2. TCP 채팅 (로그인 필수)
        Socket socket = new Socket("localhost", 10000);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        // 첫 메시지: 반드시 "LOGIN:<username>:<port>" 전송
        out.println("LOGIN:" + username + ":" + port);

        // 수신 스레드
        new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    System.out.println(msg);
                }
            } catch (Exception ignore) {}
        }).start();

        // 송신 루프
        while (true) {
            String line = sc.nextLine();
            if ("exit".equalsIgnoreCase(line)) break;
            if (line.equals("/list")) {
                // RegisterServer에 UDP로 /list 요청
                try (DatagramSocket ds = new DatagramSocket()) {
                    byte[] req = "/list".getBytes();
                    DatagramPacket p = new DatagramPacket(req, req.length, InetAddress.getByName("localhost"), 9000);
                    ds.send(p);
                    byte[] ans = new byte[2048];
                    DatagramPacket ap = new DatagramPacket(ans, ans.length);
                    ds.receive(ap);
                    System.out.println("[등록 서버 유저 목록]");
                    System.out.println(new String(ans, 0, ap.getLength()));
                }
            } else {
                out.println(line);
            }
        }

        socket.close();
        System.exit(0);
    }
}

