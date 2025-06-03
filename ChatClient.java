import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChatClient {
    private static final int REGISTER_SERVER_TCP_PORT = 8888;
    private static final int REGISTER_SERVER_UDP_PORT = 8889;
    private static final String REGISTER_SERVER_IP = "localhost";

    private static ScheduledExecutorService heartbeatScheduler;
    private static UdpNotificationListener udpNotificationListener; // UDP 알림 리스너 인스턴스

    public static void main(String[] args) {
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        String myId = null;
        int myPort = -1;
        ChatServer myChatServer = null;

        try {
            System.out.print("Enter your ID: ");
            myId = stdIn.readLine();
            if (myId == null || myId.trim().isEmpty()) {
                System.out.println("ID를 입력해야 합니다.");
                return;
            }

            System.out.print("Enter your port number for chatting and notifications: "); // 알림 수신 포트도 동일하게 사용
            String portStr = stdIn.readLine();
            try {
                myPort = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                System.out.println("유효하지 않은 포트 번호입니다. 숫자를 입력하세요.");
                return;
            }

            String myIp = "127.0.0.1";
            try {
                InetAddress localHost = InetAddress.getLocalHost();
                myIp = localHost.getHostAddress();
            } catch (UnknownHostException e) {
                System.err.println("로컬 IP 주소를 가져올 수 없습니다. 기본값 " + myIp + "를 사용합니다.");
            }

            // 내 채팅 서버 동작 (상대방이 접속할 수 있게)
            myChatServer = new ChatServer(myPort);
            myChatServer.start();
            Thread.sleep(100);

            // UDP 알림 리스너 시작 (본인 포트에서 수신)
            udpNotificationListener = new UdpNotificationListener(myPort);
            udpNotificationListener.start();
            Thread.sleep(100);

            // RegisterServer에 등록 (TCP)
            try (Socket regSocket = new Socket(REGISTER_SERVER_IP, REGISTER_SERVER_TCP_PORT);
                 PrintWriter regOut = new PrintWriter(regSocket.getOutputStream(), true);
                 BufferedReader regIn = new BufferedReader(new InputStreamReader(regSocket.getInputStream()))) {

                regOut.println("REGISTER " + myId + " " + myIp + " " + myPort);
                String regResponse = regIn.readLine();
                if ("OK".equals(regResponse)) {
                    System.out.println("RegisterServer에 성공적으로 등록되었습니다.");
                } else {
                    System.out.println("RegisterServer 등록 실패: " + regResponse);
                    return;
                }
            } catch (IOException e) {
                System.err.println("RegisterServer에 연결할 수 없습니다. 서버가 실행 중인지 확인하세요.");
                return;
            }

            // UDP Heartbeat 전송 시작
            startHeartbeat(myId);

            System.out.print("Enter target ID: ");
            String targetId = stdIn.readLine();
            if (targetId == null || targetId.trim().isEmpty()) {
                System.out.println("상대방 ID를 입력해야 합니다.");
                return;
            }

            try (Socket querySocket = new Socket(REGISTER_SERVER_IP, REGISTER_SERVER_TCP_PORT);
                 PrintWriter queryOut = new PrintWriter(querySocket.getOutputStream(), true);
                 BufferedReader queryIn = new BufferedReader(new InputStreamReader(querySocket.getInputStream()))) {

                queryOut.println("QUERY " + targetId);
                String resp = queryIn.readLine();

                if (resp != null && resp.startsWith("FOUND")) {
                    String[] tokens = resp.split(" ");
                    if (tokens.length < 3) {
                        System.out.println("RegisterServer에서 유효하지 않은 응답을 받았습니다.");
                        return;
                    }
                    String ip = tokens[1];
                    int port = Integer.parseInt(tokens[2]);
                    System.out.println("상대방: " + ip + ":" + port + " 연결 시도...");

                    try (Socket chatSocket = new Socket(ip, port);
                         BufferedReader in = new BufferedReader(new InputStreamReader(chatSocket.getInputStream()));
                         PrintWriter out = new PrintWriter(chatSocket.getOutputStream(), true)) {

                        System.out.println("채팅을 시작합니다. 'exit' 입력 시 종료.");

                        Thread receiveThread = new Thread(() -> {
                            String msg;
                            try {
                                while ((msg = in.readLine()) != null) {
                                    System.out.println("상대: " + msg);
                                }
                            } catch (IOException e) {
                                System.out.println("채팅 연결이 종료되었습니다.");
                            }
                        });
                        receiveThread.start();

                        String msg;
                        while ((msg = stdIn.readLine()) != null) {
                            if (msg.equalsIgnoreCase("exit")) {
                                System.out.println("채팅을 종료합니다.");
                                break;
                            }
                            out.println(msg);

                            try (Socket logSocket = new Socket(REGISTER_SERVER_IP, REGISTER_SERVER_TCP_PORT);
                                 PrintWriter logOut = new PrintWriter(logSocket.getOutputStream(), true)) {
                                logOut.println("MESSAGE " + myId + " " + targetId + " " + msg);
                            } catch (IOException e) {
                                System.err.println("채팅 로그를 RegisterServer에 보낼 수 없습니다: " + e.getMessage());
                            }
                        }
                        receiveThread.interrupt();
                        receiveThread.join();
                    } catch (ConnectException e) {
                        System.out.println("상대방에 연결할 수 없습니다. 상대방이 온라인 상태인지 확인하세요.");
                    } catch (IOException e) {
                        System.err.println("채팅 중 오류 발생: " + e.getMessage());
                    }
                } else if ("NOT_FOUND".equals(resp)) {
                    System.out.println("User not found: " + targetId);
                } else {
                    System.out.println("알 수 없는 응답: " + resp);
                }
            } catch (IOException e) {
                System.err.println("RegisterServer에 쿼리 중 오류 발생: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("입력/출력 오류: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("메인 스레드가 중단되었습니다: " + e.getMessage());
        } finally {
            // 하트비트 스케줄러 종료
            if (heartbeatScheduler != null) {
                heartbeatScheduler.shutdownNow();
                System.out.println("Heartbeat 스케줄러를 종료합니다.");
            }
            // UDP 알림 리스너 종료
            if (udpNotificationListener != null) {
                udpNotificationListener.shutdown();
                System.out.println("UDP 알림 리스너를 종료합니다.");
            }

            // RegisterServer에서 등록 해제 (TCP)
            if (myId != null) {
                try (Socket regSocket = new Socket(REGISTER_SERVER_IP, REGISTER_SERVER_TCP_PORT);
                     PrintWriter regOut = new PrintWriter(regSocket.getOutputStream(), true);
                     BufferedReader regIn = new BufferedReader(new InputStreamReader(regSocket.getInputStream()))) {
                    regOut.println("DEREGISTER " + myId);
                    System.out.println("RegisterServer로부터 등록 해제 응답: " + regIn.readLine());
                } catch (IOException e) {
                    System.err.println("RegisterServer에 등록 해제 요청 중 오류 발생: " + e.getMessage());
                }
            }
            // ChatServer 종료
            if (myChatServer != null) {
                myChatServer.shutdown();
                try {
                    myChatServer.join(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            try {
                stdIn.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void sendHeartbeat(String myId) {
        try (DatagramSocket socket = new DatagramSocket()) {
            String heartbeatMsg = "HEARTBEAT " + myId;
            byte[] sendBuffer = heartbeatMsg.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length,
                                                            InetAddress.getByName(REGISTER_SERVER_IP), REGISTER_SERVER_UDP_PORT);
            socket.send(sendPacket);
        } catch (IOException e) {
            System.err.println("Heartbeat 전송 실패: " + e.getMessage());
        }
    }

    private static void startHeartbeat(String myId) {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        heartbeatScheduler.scheduleAtFixedRate(() -> sendHeartbeat(myId), 0, 10, TimeUnit.SECONDS);
        System.out.println("Heartbeat 전송을 시작합니다 (10초마다).");
    }

    // UDP 알림을 수신하는 내부 클래스
    static class UdpNotificationListener extends Thread {
        private int port;
        private DatagramSocket socket;
        private byte[] buffer = new byte[512]; // 알림 메시지 버퍼

        public UdpNotificationListener(int port) {
            this.port = port;
        }

        public void run() {
            try {
                socket = new DatagramSocket(port); // 클라이언트의 채팅 서버 포트와 동일한 포트 사용
                System.out.println("UDP 알림 리스너가 포트 " + port + "에서 대기 중입니다...");

                while (!Thread.currentThread().isInterrupted()) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet); // 알림 메시지 수신

                    String received = new String(packet.getData(), 0, packet.getLength()).trim();
                    if (received.startsWith("LOGOUT_EXPLICIT ")) {
                        String id = received.substring("LOGOUT_EXPLICIT ".length()).trim();
                        System.out.println("[알림] " + id + "님이 채팅방을 나갔습니다.");
                    } else if (received.startsWith("LOGOUT_INACTIVE ")) {
                        String id = received.substring("LOGOUT_INACTIVE ".length()).trim();
                        System.out.println("[알림] " + id + "님이 오랫동안 활동이 없어 채팅방에서 나갔습니다.");
                    } else if (received.startsWith("LOGIN ")) { // 새로운 로그인 알림 처리 (선택 사항)
                        String id = received.substring("LOGIN ".length()).trim();
                        System.out.println("[알림] " + id + "님이 채팅방에 입장했습니다.");
                    }
                    // 다른 종류의 알림도 여기에서 처리 가능
                }
            } catch (SocketException e) {
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("UDP 알림 리스너가 종료되었습니다.");
                } else {
                    System.err.println("UDP 알림 소켓 오류: " + e.getMessage());
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
