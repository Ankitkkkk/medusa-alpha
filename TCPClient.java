import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class TCPClient {
    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public static void main(String[] args) {
        String hostname = "3.109.158.47"; // Replace with your server IP
        int port = 80;
        AtomicBoolean running = new AtomicBoolean(false);

        try (
            Socket socket = new Socket(hostname, port);
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))
        ) {
            running.set(true);

            // Heartbeat thread
            executor.submit(() -> {
                try {
                    while (running.get()) {
                        Thread.sleep(30000); // 30 seconds
                        output.write(0x01);  // Send raw byte
                        output.flush();
                    }
                } catch (Exception e) {
                    System.out.println("Heartbeat error: " + e.getMessage());
                }
            });

            System.out.println("Connected to the server. Type messages to send:");
            String message;
            while (true) {
                System.out.print("message for server: ");
                message = consoleReader.readLine();

                if (message == null || "exit".equalsIgnoreCase(message)) {
                    break;
                }

                writer.println(message); // Send message
            }

            running.set(false);
            System.out.println("Disconnected from the client.");

        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        } finally {
            running.set(false);
        }
    }
}
