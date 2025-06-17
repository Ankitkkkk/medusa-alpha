import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class HolePunchClient {
    private static final int TIMEOUT_MS = 5000;
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static void main(String[] args) throws Exception {
        String serverIp = "13.204.76.236";
        int serverPort = 80;
        int p2pPort = 50000 + (int) (Math.random() * 1000); // Pick an arbitrary port

        // Open a DatagramSocket early to bind NAT
        Socket punchSocket = new Socket();
        punchSocket.bind(new InetSocketAddress(p2pPort));

        Socket serverSocket = new Socket(serverIp, serverPort);
        BufferedReader in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
        PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);

        // Tell server our local port (so we can reuse it)
        out.println("HELLO:" + p2pPort);

        String response = in.readLine();
        if (response == null || !response.startsWith("PEER:")) {
            System.err.println("Invalid server response");
            return;
        }

        String[] parts = response.split(":");
        String peerIp = parts[1];
        int peerPort = Integer.parseInt(parts[2]);
        System.out.println("Got peer info: " + peerIp + ":" + peerPort);

        // Set up server socket to listen on the same port
        ServerSocket listener = new ServerSocket(p2pPort);
        listener.setSoTimeout(TIMEOUT_MS);

        // Attempt to connect to peer
        Future<Socket> connectionFuture = executor.submit(() -> {
            try {
                Socket socket = new Socket();
                socket.bind(new InetSocketAddress(p2pPort));
                socket.connect(new InetSocketAddress(peerIp, peerPort), TIMEOUT_MS);
                return socket;
            } catch (IOException e) {
                return null;
            }
        });

        Socket peerSocket = null;

        // Simultaneously accept connection from peer
        try {
            peerSocket = listener.accept();
        } catch (SocketTimeoutException e) {
            // timeout while accepting
        }

        // Wait for outbound connection attempt
        if (peerSocket == null) {
            peerSocket = connectionFuture.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        }

        if (peerSocket != null) {
            System.out.println("✅ Connected to peer: " + peerSocket.getRemoteSocketAddress());

            BufferedReader peerIn = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()));
            PrintWriter peerOut = new PrintWriter(peerSocket.getOutputStream(), true);

            executor.submit(() -> {
                try {
                    String line;
                    while ((line = peerIn.readLine()) != null) {
                        System.out.println("[Peer]: " + line);
                    }
                } catch (IOException ignored) {}
            });

            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            String msg;
            while ((msg = console.readLine()) != null) {
                peerOut.println(msg);
            }
        } else {
            System.err.println("❌ Failed to connect to peer.");
        }

        punchSocket.close();
        listener.close();
        serverSocket.close();
    }
}
