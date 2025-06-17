import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.util.concurrent.*;

public class TimestampEchoServer {
    private static final int PORT = 6789; // You can change this port as needed
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("✅ Echo server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("🔗 New client connected: " + clientSocket.getRemoteSocketAddress());

                executor.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("❌ Server error: " + e.getMessage());
        } finally {
            executor.shutdownNow();
        }
    }

    private static void handleClient(Socket socket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String message;
            while ((message = in.readLine()) != null) {
                String timestamped = "[Echo " + LocalTime.now() + "] " + message;
                System.out.println("📥 Received from " + socket.getRemoteSocketAddress() + ": " + message);
                out.println(timestamped);
            }
            System.out.println("❎ Client disconnected: " + socket.getRemoteSocketAddress());
        } catch (IOException e) {
            System.err.println("⚠️ Connection error: " + e.getMessage());
        }
    }
}
