import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RegisterServer {
    private static final int TCP_PORT = 8888;
    private static final int UDP_PORT = 8889;
    private static Map<String, UserInfo> users = new ConcurrentHashMap<>();
    private static final MessageLog messageLogger = new MessageLog("chat_log.txt");

    private static final long INACTIVE_THRESHOLD_MS = 30 * 1000;

    // UDP 알림 전송을 위한 DatagramSocket
    private static DatagramSocket udpSenderSocket;

    public static void main(String[] args) throws IOException {
        // UDP 발신 소켓 초기화
        try {
            udpSenderSocket = new DatagramSocket(); // 발신용 소켓은 특정 포트에 바인딩할 필요 없음
        } catch (SocketException e) {
            System.err.println("UDP 발신 소켓 생성 오류: " + e.getMessage());
            return; // 서버 시작 불가
        }

        ServerSocket tcpServerSocket = new ServerSocket(TCP_PORT);
        System.out.println("RegisterServer (TCP)가 포트 " + TCP_PORT + "에서 대기 중입니다...");

        new UdpHeartbeatListener(UDP_PORT).start();

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            removeInactiveUsers();
            printUserList();
        }, 0, 10, TimeUnit.SECONDS);

        printUserList();

        while (true) {
            Socket socket = tcpServerSocket.accept();
            new RegisterHandler(socket).start();
        }
    }

    private static synchronized void printUserList() {
        System.out.println("\n==== 현재 등록된 사용자 목록 ====");
        if (users.isEmpty()) {
            System.out.println("등록된 사용자가 없습니다.");
        } else {
            for (UserInfo u : users.values()) {
                System.out.println(u);
            }
        }
        System.out.println("===============================\n");
    }

    private static void removeInactiveUsers() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<String, UserInfo>> iterator = users.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, UserInfo> entry = iterator.next();
            UserInfo userInfo = entry.getValue();
            if (currentTime - userInfo.getLastActiveTime() > INACTIVE_THRESHOLD_MS) {
                String removedId = userInfo.getId();
                iterator.remove(); // ConcurrentHashMap의 remove(key, value) 대신 Iterator.remove() 사용
                System.out.println("[시스템] 사용자 " + removedId + "가 " + INACTIVE_THRESHOLD_MS / 1000 + "초 이상 비활성 상태로 감지되어 목록에서 제거되었습니다.");
                // 비활성 사용자 제거 시에도 알림 (선택 사항)
                sendUdpNotification("LOGOUT_INACTIVE " + removedId);
            }
        }
    }

    // UDP 알림 전송 메서드
    private static void sendUdpNotification(String message) {
        // 모든 등록된 사용자에게 알림을 보냅니다.
        // 현재 users 맵에 있는 모든 사용자에게 개별적으로 UDP 패킷을 보낼 수 있습니다.
        // (브로드캐스트는 네트워크 설정에 따라 다를 수 있으므로 개별 전송이 더 일반적)
        byte[] sendData = message.getBytes();
        for (UserInfo user : users.values()) {
            try {
                // RegisterServer가 클라이언트에게 UDP 알림을 보낼 때, 클라이언트가 수신 대기하는 포트(클라이언트의 자체 ChatServer 포트와 다를 수 있음)로 보내야 합니다.
                // 여기서는 클라이언트의 'myPort'를 알림 수신 포트로 가정합니다.
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                                                               InetAddress.getByName(user.getIp()), user.getPort()); // user.getPort() 사용
                udpSenderSocket.send(sendPacket);
                // System.out.println("UDP 알림 전송: " + message + " to " + user.getId()); // 디버그용
            } catch (IOException e) {
                System.err.println("UDP 알림 전송 실패 (" + user.getId() + "): " + e.getMessage());
            }
        }
        // 로그아웃 알림은 전체적으로 1번만 출력
        if (message.startsWith("LOGOUT")) {
            System.out.println("[알림] UDP로 '" + message + "' 알림 전송 완료.");
        }
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
                    if (tokens.length < 4) {
                        out.println("ERROR Invalid REGISTER format");
                        return;
                    }
                    String id = tokens[1];
                    String ip = tokens[2];
                    int port = Integer.parseInt(tokens[3]);
                    UserInfo newUser = new UserInfo(id, ip, port);
                    users.put(id, newUser);
                    System.out.println("등록 완료: " + id + " -> " + ip + ":" + port);
                    out.println("OK");
                    newUser.setLastActiveTime(System.currentTimeMillis());
                    printUserList();
                    // 새로운 사용자 로그인 시 다른 사용자들에게 알림 (선택 사항)
                    sendUdpNotification("LOGIN " + id);
                } else if (request.startsWith("QUERY")) {
                    String[] tokens = request.split(" ");
                     if (tokens.length < 2) {
                        out.println("ERROR Invalid QUERY format");
                        return;
                    }
                    String id = tokens[1];
                    UserInfo user = users.get(id);
                    if (user != null) {
                        System.out.println("쿼리 성공: " + id + " -> " + user.getIp() + ":" + user.getPort());
                        out.println("FOUND " + user.getIp() + " " + user.getPort());
                        user.setLastActiveTime(System.currentTimeMillis());
                    } else {
                        System.out.println("쿼리 실패: " + id);
                        out.println("NOT_FOUND");
                    }
                    printUserList();
                } else if (request.startsWith("MESSAGE")) {
                    String[] tokens = request.split(" ", 4);
                    if (tokens.length < 3) {
                        System.out.println("ERROR Invalid MESSAGE format");
                        return;
                    }
                    String sender = tokens[1];
                    String target = tokens[2];
                    String content = tokens.length >= 4 ? tokens[3] : "";
                    String logEntry = String.format("[%s] %s -> %s : %s",
                                                    new Date().toString(), sender, target, content);
                    System.out.println("[채팅로그] " + logEntry);
                    messageLogger.log(logEntry);

                    UserInfo senderInfo = users.get(sender);
                    if (senderInfo != null) {
                        senderInfo.setLastActiveTime(System.currentTimeMillis());
                    }
                } else if (request.startsWith("DEREGISTER")) {
                    String[] tokens = request.split(" ");
                    if (tokens.length < 2) {
                        out.println("ERROR Invalid DEREGISTER format");
                        return;
                    }
                    String id = tokens[1];
                    if (users.remove(id) != null) {
                        System.out.println("등록 해제 완료: " + id);
                        out.println("OK");
                        // 등록 해제 알림을 UDP로 전송
                        sendUdpNotification("LOGOUT_EXPLICIT " + id);
                    } else {
                        System.out.println("등록되지 않은 사용자 해제 요청: " + id);
                        out.println("NOT_REGISTERED");
                    }
                    printUserList();
                } else {
                    System.out.println("알 수 없는 요청: " + request);
                    out.println("ERROR Unknown command");
                }
            } catch (NumberFormatException e) {
                System.err.println("잘못된 포트 번호: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("클라이언트 통신 오류: " + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // UDP Heartbeat 리스너 스레드 (기존과 동일)
    static class UdpHeartbeatListener extends Thread {
        private int port;
        private DatagramSocket socket;
        private byte[] buffer = new byte[256];

        public UdpHeartbeatListener(int port) {
            this.port = port;
        }

        public void run() {
            try {
                socket = new DatagramSocket(port);
                System.out.println("RegisterServer (UDP)가 포트 " + port + "에서 Heartbeat를 대기 중입니다...");

                while (!Thread.currentThread().isInterrupted()) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String received = new String(packet.getData(), 0, packet.getLength()).trim();
                    if (received.startsWith("HEARTBEAT ")) {
                        String id = received.substring("HEARTBEAT ".length()).trim();
                        UserInfo user = users.get(id);
                        if (user != null) {
                            user.setLastActiveTime(System.currentTimeMillis());
                            // System.out.println("Heartbeat 수신: " + id + " (활성 시간 업데이트)");
                        } else {
                            System.out.println("등록되지 않은 사용자로부터의 Heartbeat 수신: " + id);
                        }
                    } else {
                        System.out.println("알 수 없는 UDP 메시지: " + received + " from " + packet.getAddress() + ":" + packet.getPort());
                    }
                }
            } catch (SocketException e) {
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("UDP Heartbeat 리스너가 종료되었습니다.");
                } else {
                    System.err.println("UDP 소켓 오류: " + e.getMessage());
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            }
        }

        public void shutdown() {
            interrupt();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }
}
