import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 10000;
    private static Map<String, ClientHandler> clientMap = Collections.synchronizedMap(new HashMap<>());
    private static Map<Integer, ClientHandler> portMap = Collections.synchronizedMap(new HashMap<>());
    private static MessageLog log = new MessageLog();
    private static PrintWriter logWriter;

    public static void main(String[] args) throws Exception {
        ServerSocket server = new ServerSocket(PORT);
        logWriter = new PrintWriter(new FileWriter("chatlog.txt", true), true); // 파일 로그용

        System.out.println("ChatServer(TCP) 대기중...");

        while (true) {
            Socket client = server.accept();
            new Thread(new ClientHandler(client)).start();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private String username;
        private int portNum = -1;
        private PrintWriter out;

        public ClientHandler(Socket socket) { this.socket = socket; }

        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // 첫 줄은 반드시 "LOGIN:<username>:<port>"
                String first = in.readLine();
                if (first == null || !first.startsWith("LOGIN:")) {
                    out.println("FAIL:등록 후 채팅 접속 필요!"); socket.close(); return;
                }
                String[] parts = first.substring(6).trim().split(":");
                if (parts.length != 2) {
                    out.println("FAIL:로그인 형식오류"); socket.close(); return;
                }
                username = parts[0];
                try { portNum = Integer.parseInt(parts[1]); }
                catch (Exception e) {
                    out.println("FAIL:포트번호 오류"); socket.close(); return;
                }

                // 이름 또는 포트 중복 로그인 체크
                if (clientMap.containsKey(username)) {
                    ClientHandler old = clientMap.get(username);
                    old.out.println("[System][중복로그인] 동일 이름으로 다른 세션이 접속하여 연결 종료됨.");
                    old.close();
                    clientMap.remove(username);
                    portMap.values().removeIf(h -> h == old);
                }
                if (portMap.containsKey(portNum)) {
                    ClientHandler old = portMap.get(portNum);
                    old.out.println("[System][중복로그인] 동일 포트번호로 다른 세션이 접속하여 연결 종료됨.");
                    old.close();
                    portMap.remove(portNum);
                    clientMap.values().removeIf(h -> h == old);
                }

                clientMap.put(username, this);
                portMap.put(portNum, this);
                logAndPrint("[입장] " + username + "님이 입장하셨습니다.", true);

                String msg;
                while ((msg = in.readLine()) != null) {
                    if (msg.equals("/users")) {
                        out.println("[System] 접속자 목록: " + String.join(", ", clientMap.keySet()));
                    } else if (msg.equals("/history")) {
                        List<String> history = log.getAll();
                        int N = Math.min(10, history.size());
                        out.println("[System] 최근 대화 기록:");
                        for (int i = history.size()-N; i < history.size(); i++)
                            out.println(history.get(i));
                    } else if (msg.startsWith("/search ")) {
                        String keyword = msg.substring(8);
                        out.println("[System] '" + keyword + "'로 검색:");
                        for (String m : log.getAll())
                            if (m.contains(keyword)) out.println(m);
                    } else {
                        logAndPrint("[" + username + "] " + msg, false);
                    }
                }
            } catch (Exception e) {}
            finally {
                if (username != null) {
                    clientMap.remove(username);
                    portMap.remove(portNum);
                    logAndPrint("[퇴장] " + username + "님이 퇴장하셨습니다.", true);
                }
                close();
            }
        }

        private void logAndPrint(String msg, boolean systemMsg) {
            String outMsg = systemMsg ? "[System] " + msg : msg;
            log.add(outMsg);
            System.out.println(outMsg); // 콘솔 실시간 출력
            synchronized (logWriter) { logWriter.println(outMsg); }
            broadcast(outMsg, systemMsg);
        }

        private void broadcast(String msg, boolean systemMsg) {
            synchronized (clientMap) {
                for (ClientHandler c : clientMap.values())
                    c.out.println(msg);
            }
        }
        private void close() { try { socket.close(); } catch (Exception ignore) {} }
    }
}

