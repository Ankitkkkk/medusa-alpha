import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class TCPServer {
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        int port = 6789;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getInetAddress());
                executor.submit(() -> handleClient(socket));
            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
        }
    }

    private static void handleClient(Socket socket) {
        try (
            InputStream input = socket.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(input)
        ) {
            byte[] buffer = new byte[2048];
            int bytesRead;

            while ((bytesRead = bis.read(buffer)) != -1) {
                for (int i = 0; i < bytesRead; i++) {
                    byte b = buffer[i];

                    if (b == 0x01) {
                        // Heartbeat received
                        System.out.println("Heartbeat received from " + socket.getInetAddress());
                    } else {
                        // Treat as part of a message
                        String message = new String(new byte[]{b}, "UTF-8");
                        System.out.print(message); // Print without newline
                    }
                }
            }

        } catch (IOException ex) {
            System.out.println("Client disconnected: " + socket.getInetAddress());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Error closing socket: " + e.getMessage());
            }
            System.out.println("Connection closed...");
        }
    }
}
