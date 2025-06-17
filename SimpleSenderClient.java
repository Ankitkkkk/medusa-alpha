import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.util.concurrent.*;

public class SimpleSenderClient {
    private static final String TARGET_IP = "223.190.82.148";   // Change to peer IP
    private static final int TARGET_PORT = 10969;           // Change to target port

    public static void main(String[] args) throws Exception {
        try (Socket socket = new Socket(TARGET_IP, TARGET_PORT)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            while (true) {
             String message = "hlw world " + LocalTime.now();
                out.println(message);
                System.out.println("[Sent]: " + message);
                Thread.sleep(5000);   
            }
        }
    }
}
