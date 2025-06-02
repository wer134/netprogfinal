import java.io.*;
import java.net.*;

public class ChatServer extends Thread {
    private int port;
    public ChatServer(int port) { this.port = port; }
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("채팅 서버가 포트 " + port + "에서 대기 중...");
            Socket socket = serverSocket.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String msg;
            while ((msg = in.readLine()) != null) {
                System.out.println("상대: " + msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

