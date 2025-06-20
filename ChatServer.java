import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 10123;

    // username -> 클라이언트 핸들러
    private static final Map<String, ClientHandler> clientMap = new ConcurrentHashMap<>();
    // 채팅 세션: username -> 상대방 username
    private static final Map<String, String> chatSessions = new ConcurrentHashMap<>();
    // 채팅 요청 대기: target -> (요청자, 타이머)
    private static final Map<String, PendingRequest> pendingRequests = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("[ChatServer] 채팅 서버 시작 (포트: " + PORT + ")");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(new ClientHandler(clientSocket)).start();
        }
    }

    static class PendingRequest {
        String fromUser;
        ScheduledFuture<?> timeoutTask;
        PendingRequest(String fromUser, ScheduledFuture<?> timeoutTask) {
            this.fromUser = fromUser;
            this.timeoutTask = timeoutTask;
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private String username = null;
        private BufferedReader in;
        private PrintWriter out;

        public ClientHandler(Socket socket) { this.socket = socket; }

        public void send(String msg) { out.println(msg); }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // 최초 유저명 수신
                out.println("이름 입력:");
                username = in.readLine().trim();
                if (username == null || username.isEmpty() || clientMap.containsKey(username)) {
                    out.println("[System] 잘못된 이름이거나 중복된 사용자입니다. 종료합니다.");
                    socket.close();
                    return;
                }
                clientMap.put(username, this);

                // 입장 알림
                broadcastExcept("[System] [입장] " + username + "님이 입장하셨습니다.", username);

                out.println("[System] [입장] " + username + "님이 입장하셨습니다.");
                out.println("명령어 안내: /user(유저목록) /chat [이름](채팅요청) /exit(1:1채팅종료)");

                String msg;
                while ((msg = in.readLine()) != null) {
                    msg = msg.trim();
                    if (msg.equalsIgnoreCase("/user")) {
                        out.println("[System] 현재 접속자: " + String.join(", ", clientMap.keySet()));
                    }
                    else if (msg.startsWith("/chat ")) {
                        String target = msg.substring(6).trim();
                        if (!clientMap.containsKey(target)) {
                            out.println("[System] 해당 사용자가 존재하지 않습니다.");
                        } else if (target.equals(username)) {
                            out.println("[System] 본인에게는 요청 불가.");
                        } else if (chatSessions.containsKey(username) || chatSessions.containsKey(target)) {
                            out.println("[System] 이미 채팅중인 사용자가 있습니다.");
                        } else if (pendingRequests.containsKey(target)) {
                            out.println("[System] 해당 사용자에게 이미 요청이 있습니다.");
                        } else {
                            // 채팅 요청 보냄
                            ClientHandler targetHandler = clientMap.get(target);
                            targetHandler.send("[System] " + username + "님이 채팅을 요청하셨습니다. 연결하려면 /chat, 원하지 않으면 아무키나 입력하세요.");
                            out.println("[System] " + target + "님에게 채팅 요청을 보냈습니다. 응답을 기다리는 중...");

                            ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
                            ScheduledFuture<?> timeoutTask = exec.schedule(() -> {
                                // 10초 응답 없음
                                if (pendingRequests.containsKey(target)) {
                                    pendingRequests.remove(target);
                                    out.println("[System] " + target + "님이 응답하지 않아 채팅 요청이 취소되었습니다.");
                                    targetHandler.send("[System] 채팅 요청 시간이 초과되어 자동 거절되었습니다.");
                                }
                                exec.shutdown();
                            }, 10, TimeUnit.SECONDS);
                            pendingRequests.put(target, new PendingRequest(username, timeoutTask));
                        }
                    }
                    else if (msg.equalsIgnoreCase("/chat")) {
                        // 채팅 요청에 대한 응답(수락)
                        PendingRequest pr = pendingRequests.get(username);
                        if (pr != null) {
                            pendingRequests.remove(username);
                            pr.timeoutTask.cancel(true);
                            String fromUser = pr.fromUser;
                            chatSessions.put(username, fromUser);
                            chatSessions.put(fromUser, username);
                            clientMap.get(fromUser).send("[System] " + username + "님과 1:1 채팅이 시작됩니다.");
                            out.println("[System] " + fromUser + "님과 1:1 채팅이 시작됩니다.");
                        } else {
                            out.println("[System] 채팅 요청이 없습니다.");
                        }
                    }
                    else if (msg.equalsIgnoreCase("/exit")) {
                        // 1:1 채팅 종료
                        if (chatSessions.containsKey(username)) {
                            String partner = chatSessions.get(username);
                            chatSessions.remove(username);
                            chatSessions.remove(partner);
                            clientMap.get(partner).send("[System] " + username + "님이 1:1 채팅을 종료했습니다. 공용방으로 복귀합니다.");
                            out.println("[System] 1:1 채팅이 종료되어 공용방으로 복귀합니다.");
                        } else {
                            out.println("[System] 1:1 채팅 세션이 아닙니다.");
                        }
                    }
                    else {
                        // 채팅 메시지 처리
                        if (chatSessions.containsKey(username)) {
                            String partner = chatSessions.get(username);
                            if (clientMap.containsKey(partner)) {
                                clientMap.get(partner).send("[1:1][" + username + "] " + msg);
                                out.println("[1:1][" + username + "] " + msg);
                            } else {
                                out.println("[System] 상대방이 접속 종료됨. 1:1 세션 종료.");
                                chatSessions.remove(username);
                            }
                        } else {
                            // 공용방: 전체에 브로드캐스트
                            broadcastExcept("[" + username + "]: " + msg, null);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[오류] " + username + ": " + e.getMessage());
            } finally {
                try {
                    if (username != null) {
                        clientMap.remove(username);
                        // 세션/요청 정리
                        if (chatSessions.containsKey(username)) {
                            String partner = chatSessions.get(username);
                            chatSessions.remove(username);
                            chatSessions.remove(partner);
                            if (clientMap.containsKey(partner))
                                clientMap.get(partner).send("[System] 상대방이 접속 종료함. 1:1 세션 종료.");
                        }
                        pendingRequests.remove(username);
                        broadcastExcept("[System] [퇴장] " + username + "님이 퇴장하셨습니다.", username);
                    }
                    socket.close();
                } catch (Exception ignored) {}
            }
        }

        // 전체 브로드캐스트 (except는 제외)
        private void broadcastExcept(String msg, String except) {
            for (Map.Entry<String, ClientHandler> entry : clientMap.entrySet()) {
                if (except == null || !entry.getKey().equals(except)) {
                    entry.getValue().send(msg);
                }
            }
        }
    }
}

