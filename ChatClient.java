import java.io.*;
import java.net.*;

public class ChatClient {
    public static void main(String[] args) throws Exception {
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

        // Register Server에 내 정보 등록
        System.out.print("Enter your ID: ");
        String myId = stdIn.readLine();

        System.out.print("Enter your port number for chatting: ");
        int myPort = Integer.parseInt(stdIn.readLine());

        // 내 채팅 서버 동작 (상대방이 접속할 수 있게)
        new ChatServer(myPort).start();

        // RegisterServer에 등록
        Socket regSocket = new Socket("localhost", 8888);
        PrintWriter regOut = new PrintWriter(regSocket.getOutputStream(), true);
        BufferedReader regIn = new BufferedReader(new InputStreamReader(regSocket.getInputStream()));
        regOut.println("REGISTER " + myId + " 127.0.1.1 " + myPort);
        regIn.readLine(); // "OK"
        regSocket.close();

        // 상대방 ID 입력받아 RegisterServer에 쿼리
        System.out.print("Enter target ID: ");
        String targetId = stdIn.readLine();

        Socket querySocket = new Socket("localhost", 8888);
        PrintWriter queryOut = new PrintWriter(querySocket.getOutputStream(), true);
        BufferedReader queryIn = new BufferedReader(new InputStreamReader(querySocket.getInputStream()));
        queryOut.println("QUERY " + targetId);

        String resp = queryIn.readLine();
        if (resp.startsWith("FOUND")) {
            String[] tokens = resp.split(" ");
            String ip = tokens[1];
            int port = Integer.parseInt(tokens[2]);
            System.out.println("상대방: " + ip + ":" + port + " 연결 시도...");

            // 소켓 연결해서 메시지 송수신
            Socket chatSocket = new Socket(ip, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(chatSocket.getInputStream()));
            PrintWriter out = new PrintWriter(chatSocket.getOutputStream(), true);

            // 송수신 쓰레드 분리
            Thread receive = new Thread(() -> {
                String msg;
                try {
                    while ((msg = in.readLine()) != null) {
                        System.out.println("상대: " + msg);
                    }
                } catch (IOException e) { }
            });
            receive.start();

            String msg;
            while ((msg = stdIn.readLine()) != null) {
                out.println(msg);

                // RegisterServer로 채팅 로그 보내기
                Socket logSocket = new Socket("localhost", 8888);
                PrintWriter logOut = new PrintWriter(logSocket.getOutputStream(), true);
                logOut.println("MESSAGE " + myId + " " + targetId + " " + msg);
                logSocket.close();
            }
        } else {
            System.out.println("User not found");
        }
        querySocket.close();
    }
}

