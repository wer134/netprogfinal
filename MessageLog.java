import java.io.*;
import java.util.*;

public class MessageLog {
    private List<String> messages = new ArrayList<>();

    public synchronized void add(String msg) {
        messages.add(msg);
    }

    public synchronized List<String> getAll() {
        return new ArrayList<>(messages);
    }

    public synchronized void saveToFile(String filename) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
            for (String msg : messages) {
                bw.write(msg);
                bw.newLine();
            }
        }
    }

    public synchronized void loadFromFile(String filename) throws IOException {
        messages.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) messages.add(line);
        }
    }
}

