import java.io.*;
import java.net.*;

public class ChatServer extends Thread {
    private int port;
    private ServerSocket serverSocket; // 닫기 위해 참조 유지

    public ChatServer(int port) {
        this.port = port;
    }

    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("채팅 서버가 포트 " + port + "에서 대기 중...");
            // 이 P2P 모델에서는 하나의 연결만 예상되더라도 연결을 계속 수락합니다.
            // 하지만 이 예시에서는 하나의 연결만 처리합니다.
            Socket socket = serverSocket.accept(); // 연결이 이루어질 때까지 블록
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String msg;
            // 스레드가 인터럽트되거나 입력 스트림이 종료될 때까지 메시지를 읽습니다.
            while (!isInterrupted() && (msg = in.readLine()) != null) {
                System.out.println("상대: " + msg);
            }
        } catch (SocketException e) {
            // 소켓이 닫힐 때 발생하는 일반적인 예외 (shutdown() 호출 시)
            if (isInterrupted() || e.getMessage().contains("Socket closed")) {
                System.out.println("채팅 서버가 종료되었습니다.");
            } else {
                System.err.println("채팅 서버 소켓 오류: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("채팅 서버 오류: " + e.getMessage());
        } finally {
            // 소켓이 아직 열려있으면 닫습니다.
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // ChatClient에서 이 메서드를 호출하여 ChatServer를 정상적으로 종료합니다.
    public void shutdown() {
        interrupt(); // 스레드 인터럽트 요청
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close(); // accept() 호출을 언블록하기 위해 서버 소켓 닫기
            } catch (IOException e) {
                System.err.println("채팅 서버 소켓 닫기 오류: " + e.getMessage());
            }
        }
    }
}
