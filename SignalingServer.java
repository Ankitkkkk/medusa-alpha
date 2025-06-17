import java.io.*;
import java.net.*;
import java.util.*;

public class SignalingServer {
    static class ClientInfo {
        Socket socket;
        String ip;
        int port;

        ClientInfo(Socket socket, String ip, int port) {
            this.socket = socket;
            this.ip = ip;
            this.port = port;
        }
    }

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(6789);
        List<ClientInfo> waitingClients = new ArrayList<>();

        while (true) {
            Socket socket = serverSocket.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            String hello = "";
            while (true) {
                hello = in.readLine();
                System.out.println("data/port recieved: " + hello);
                if (hello.startsWith("PROXY")) {
                    continue;
                } else break;
            }
            int port = Integer.parseInt(hello.split(":")[1]);
            String ip = socket.getInetAddress().getHostAddress();

            ClientInfo client = new ClientInfo(socket, ip, port);

            synchronized (waitingClients) {
                if (waitingClients.size() >= 1) {
                    ClientInfo peer = waitingClients.remove(0);

                    PrintWriter out1 = new PrintWriter(peer.socket.getOutputStream(), true);
                    PrintWriter out2 = new PrintWriter(client.socket.getOutputStream(), true);

                    out1.println("PEER:" + client.ip + ":" + client.port);
                    out2.println("PEER:" + peer.ip + ":" + peer.port);

                    peer.socket.close();
                    client.socket.close();
                } else {
                    waitingClients.add(client);
                }
            }
        }
    }
}
