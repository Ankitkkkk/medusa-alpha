import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class TCPClient {
    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public static void main(String[] args) {
        String hostname = "13.204.76.236"; // Replace with your server IP
        int port = 80; // Change if needed
        AtomicBoolean running = new AtomicBoolean(false);

        try (
            Socket socket = new Socket(hostname, port);
            OutputStream output = socket.getOutputStream();
            InputStream input = socket.getInputStream();
            PrintWriter writer = new PrintWriter(output, true);
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            BufferedReader serverReader = new BufferedReader(new InputStreamReader(input))
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
                } finally {
                    running.set(false);
                }
            });

            // Message receiver thread
            executor.submit(() -> {
                try {
                    String serverMessage;
                    while (running.get() && (serverMessage = serverReader.readLine()) != null) {
                        System.out.println("\n[Server]: " + serverMessage);
                        System.out.print("message for server: "); // reprint prompt
                    }
                } catch (IOException e) {
                    System.out.println("Connection closed by server.");
                } finally {
                    running.set(false);
                }
            });

            // Main loop for sending messages
            System.out.println("Connected to the server. Type messages to send:");
            String message;
            while (running.get()) {
                System.out.print("message for server: ");
                message = consoleReader.readLine();

                if (message == null || "exit".equalsIgnoreCase(message)) {
                    break;
                }

                writer.println(message); // Send message
            }

            running.set(false);
            System.out.println("Disconnected from the server.");

        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        } finally {
            running.set(false);
        }
    }
}
