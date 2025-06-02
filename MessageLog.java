import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

public class MessageLog {
    private final String logFile;

    public MessageLog(String filename) {
        this.logFile = filename;
    }

    public synchronized void save(String senderId, String message, int senderPort) {
        try (FileWriter writer = new FileWriter(logFile, true)) {
            String logEntry = String.format("[%s] From %s (Port %d): %s%n",
                    LocalDateTime.now(), senderId, senderPort, message);
            writer.write(logEntry);
        } catch (IOException e) {
            System.err.println("로그 저장 실패: " + e.getMessage());
        }
    }
}

