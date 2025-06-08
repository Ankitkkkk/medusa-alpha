import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class TCPClient {
    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public static void main(String[] args) {
        String hostname = "3.109.158.47";
        // "localhost"; // or use the server's IP address
        int port = 80;
        AtomicBoolean running = new AtomicBoolean(false);
        try (
                Socket socket = new Socket(hostname, port);
                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);
                BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {
            running.set(true);
            executor.submit(() -> {
                try {
                    while (true) {
                        Thread.sleep(30000);
                        if (running.get()) {
                            writer.write(0x01);
                        } else {
                            return;
                        }
                    }
                } catch (Exception e) {
                    System.out.println("error: " + e.getMessage());
                }
            });

            System.out.println("Connected to the server. Type messages to send:");
            var message = "";
            do {
                System.out.print("message for server: ");
                message = consoleReader.readLine();

                if ("exit".equals(message)) {
                    break;
                }

                writer.println(message);
            } while (message != null);
            System.out.println("Disconnected from the client.");

        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }
}
