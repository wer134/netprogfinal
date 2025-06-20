import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MessageLog {
    private final List<String> log = new ArrayList<>();

    public void add(String from, String to, String msg) {
        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        log.add(ts + " [" + from + "->" + to + "] " + msg);
    }

    public void printAll() {
        for (String s : log) {
            System.out.println(s);
        }
    }
}

