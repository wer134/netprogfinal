import java.io.FileWriter;
import java.io.IOException;

public class MessageLog {
    private String filename;

    public MessageLog(String filename) {
        this.filename = filename;
    }

    public synchronized void log(String msg) {
        try (FileWriter fw = new FileWriter(filename, true)) {
            fw.write(msg + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

