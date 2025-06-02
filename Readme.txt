## 📁 예제 구조

```
📂project/
 ┣ 📄 RegisterServer.java  ← UDP 서버
 ┣ 📄 ChatClient.java      ← 사용자 채팅 클라이언트
 ┣ 📄 ChatServer.java      ← 사용자 채팅 서버 (Peer 연결용)
 ┣ 📄 UserInfo.java        ← 사용자 정보 클래스
 ┗ 📄 Message.java         ← 채팅 메시지 포맷 클래스
```

---

## ✅ 구현 순서

### ✅ 1. RegisterServer.java (UDP 등록 서버)

```java
public class RegisterServer {
    private static final int PORT = 8888;
    private static Map<String, String> registry = new HashMap<>();

    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(PORT);
        byte[] buffer = new byte[1024];

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            String msg = new String(packet.getData(), 0, packet.getLength());

            String response = handle(msg.trim());
            byte[] responseData = response.getBytes();
            DatagramPacket responsePacket = new DatagramPacket(
                responseData, responseData.length, packet.getAddress(), packet.getPort());
            socket.send(responsePacket);
        }
    }

    private static String handle(String msg) {
        String[] parts = msg.split(" ");
        switch (parts[0]) {
            case "REGISTER":
                registry.put(parts[1], parts[2] + ":" + parts[3]);
                return "OK";
            case "QUERY":
                return registry.containsKey(parts[1])
                        ? "FOUND " + registry.get(parts[1])
                        : "NOT_FOUND";
            default:
                return "ERROR Unknown Command";
        }
    }
}
```

---

### ✅ 2. ChatClient.java

```java
public class ChatClient {
    public static void main(String[] args) throws Exception {
        String myId = "user1";
        String myIp = InetAddress.getLocalHost().getHostAddress();
        int myPort = 9001;

        // 1. Register to RegisterServer
        DatagramSocket udpSocket = new DatagramSocket();
        String registerMsg = "REGISTER " + myId + " " + myIp + " " + myPort;
        byte[] data = registerMsg.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("localhost"), 8888);
        udpSocket.send(packet);

        // 2. Query for another user
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter target ID: ");
        String targetId = sc.nextLine();
        String query = "QUERY " + targetId;
        udpSocket.send(new DatagramPacket(query.getBytes(), query.length(), packet.getAddress(), 8888));

        DatagramPacket response = new DatagramPacket(new byte[1024], 1024);
        udpSocket.receive(response);
        String resp = new String(response.getData(), 0, response.getLength());

        if (!resp.startsWith("FOUND")) {
            System.out.println("User not found");
            return;
        }

        String[] ipPort = resp.split(" ")[1].split(":");
        Socket tcpSocket = new Socket(ipPort[0], Integer.parseInt(ipPort[1]));

        // 3. Start chatting
        new Thread(() -> {
            try (Scanner in = new Scanner(tcpSocket.getInputStream())) {
                while (in.hasNextLine())
                    System.out.println("[RECV] " + in.nextLine());
            } catch (IOException e) {}
        }).start();

        PrintWriter out = new PrintWriter(tcpSocket.getOutputStream(), true);
        while (true) {
            String line = sc.nextLine();
            out.println(line);
        }
    }
}
```

---

### ✅ 3. ChatServer.java

```java
public class ChatServer {
    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(9001);
        System.out.println("Waiting for incoming TCP connections...");

        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("Client connected from: " + socket.getInetAddress());

            new Thread(() -> {
                try (Scanner in = new Scanner(socket.getInputStream())) {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    while (in.hasNextLine()) {
                        String line = in.nextLine();
                        System.out.println("[RECV] " + line);
                        out.println("Echo: " + line);
                    }
                } catch (IOException e) {}
            }).start();
        }
    }
}
```
