import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ChatServer {
    private static final int SERVER_PORT = 9001;
    private static final MessageLog log = new MessageLog("chat_log.txt");

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
        System.out.println("TCP 채팅 서버가 포트 " + SERVER_PORT + "에서 대기 중입니다...");

        while (true) {
            Socket socket = serverSocket.accept();
            String clientAddr = socket.getInetAddress().getHostAddress();
            int clientPort = socket.getPort();
            System.out.println("클라이언트 접속: " + clientAddr + ":" + clientPort);

            new Thread(() -> {
                try (Scanner in = new Scanner(socket.getInputStream());
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                    while (in.hasNextLine()) {
                        String msg = in.nextLine();
                        log.save(clientAddr, msg, clientPort); // 로그 저장
                        System.out.println("[RECV] " + msg);
                        out.println("Echo: " + msg); // 응답
                    }

                } catch (IOException e) {
                    System.err.println("클라이언트 연결 오류: " + e.getMessage());
                }
            }).start();
        }
    }
}

