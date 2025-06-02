import java.io.*;
import java.net.*;
import java.util.*;

public class RegisterServer {
    private static final int PORT = 8888;
    private static Map<String, UserInfo> users = new HashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("RegisterServer가 포트 " + PORT + "에서 대기 중입니다...");
        printUserList();

        while (true) {
            Socket socket = serverSocket.accept();
            new RegisterHandler(socket).start();
        }
    }

    private static synchronized void printUserList() {
        System.out.println("==== 현재 등록된 사용자 목록 ====");
        if (users.isEmpty()) {
            System.out.println("등록된 사용자가 없습니다.");
        } else {
            for (UserInfo u : users.values()) {
                System.out.println(u.getId() + " -> " + u.getIp() + ":" + u.getPort());
            }
        }
        System.out.println("===============================");
    }

    static class RegisterHandler extends Thread {
        Socket socket;
        RegisterHandler(Socket socket) { this.socket = socket; }
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                String request = in.readLine();
                if (request == null) return;
                System.out.println("수신된 요청: " + request + " from " + socket.getRemoteSocketAddress());

                if (request.startsWith("REGISTER")) {
                    String[] tokens = request.split(" ");
                    String id = tokens[1], ip = tokens[2], port = tokens[3];
                    users.put(id, new UserInfo(id, ip, Integer.parseInt(port)));
                    System.out.println("등록 완료: " + id + " -> " + ip + ":" + port);
                    out.println("OK");
                    printUserList();
                } else if (request.startsWith("QUERY")) {
                    String[] tokens = request.split(" ");
                    String id = tokens[1];
                    UserInfo user = users.get(id);
                    if (user != null) {
                        System.out.println("쿼리 성공: " + id + " -> " + user.getIp() + ":" + user.getPort());
                        out.println("FOUND " + user.getIp() + " " + user.getPort());
                    } else {
                        System.out.println("쿼리 실패: " + id);
                        out.println("NOT_FOUND");
                    }
                    printUserList();
                } else if (request.startsWith("MESSAGE")) {
                    // MESSAGE senderID targetID content
                    String[] tokens = request.split(" ", 4);
                    String sender = tokens[1];
                    String target = tokens[2];
                    String content = tokens.length >= 4 ? tokens[3] : "";
                    System.out.println("[채팅로그] " + sender + " -> " + target + " : " + content);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

