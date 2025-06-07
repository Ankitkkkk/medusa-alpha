import java.io.*;
import java.net.*;

public class TCPClient {
    public static void main(String[] args) {
        String hostname = "3.110.119.249";
        // "localhost"; // or use the server's IP address
        int port = 80;

        try (
            Socket socket = new Socket(hostname, port);
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))
        ) {
            System.out.println("Connected to the server. Type messages to send:");
            do {
                System.out.print("message for server: ");
                var message = consoleReader.readLine();

                if ("exit".equals(message)) {
                    break;
                }

                writer.println(message);
            } while(true);

        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }
}
