import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.util.concurrent.*;

public class BidirectionalClient {
    private static final String SERVER_IP = "13.204.76.236"; // Server IP
    private static final int SERVER_PORT = 80;               // Server port
    private static final int CLIENT_PORT = 55550;            // Local bind port

    public static void main(String[] args) {
        ExecutorService executor = Executors.newCachedThreadPool();

        try {
            try (// Bind local port and connect to server
            Socket socket = new Socket()) {
                socket.bind(new InetSocketAddress(CLIENT_PORT));
                socket.connect(new InetSocketAddress(SERVER_IP, SERVER_PORT));

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                // Listener thread — continuously read from server
                executor.submit(() -> {
                    try {
                        String msg;
                        while ((msg = in.readLine()) != null) {
                            System.out.println("[Received]: " + msg);
                        }
                        System.out.println("Connection closed by server.");
                    } catch (IOException e) {
                        System.err.println("Read error: " + e.getMessage());
                    }
                });

                // Main thread — sends every 5 seconds
                while (true) {
                    String message = "hi " + LocalTime.now();
                    out.println(message);
                    System.out.println("[Sent]: " + message);
                    Thread.sleep(5000);
                }
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Client error: " + e.getMessage());
        } finally {
            executor.shutdownNow();
        }
    }
}
