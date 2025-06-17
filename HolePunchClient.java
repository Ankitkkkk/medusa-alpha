import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class HolePunchClient {
    private static final int TIMEOUT_MS = 10000; // 10 seconds
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static void main(String[] args) throws Exception {
        String serverIp = "13.204.76.236"; // your signaling server
        int serverPort = 80;

        int p2pPort = 50000 + (int) (Math.random() * 1000); // random port for P2P

        // Connect to signaling server
        Socket serverSocket = new Socket(serverIp, serverPort);
        BufferedReader in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
        PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);

        // Announce ourselves
        out.println("HELLO:" + p2pPort);

        String response = in.readLine();
        if (response == null || !response.startsWith("PEER:")) {
            System.err.println("Invalid server response");
            serverSocket.close();
            return;
        }

        // Parse peer IP and port
        String[] parts = response.split(":");
        String peerIp = parts[1];
        int peerPort = Integer.parseInt(parts[2]);
        System.out.println("Got peer info: " + peerIp + ":" + peerPort);

        // Set up listener (for incoming peer connection)
        ServerSocket listener = new ServerSocket(p2pPort);
        listener.setSoTimeout(TIMEOUT_MS);

        // Simultaneously try to connect to peer
        Future<Socket> outgoingFuture = executor.submit(() -> {
            try {
                Thread.sleep(500); // allow listener to be ready
                Socket socket = new Socket();
                socket.bind(new InetSocketAddress(p2pPort)); // bind to same port
                socket.connect(new InetSocketAddress(peerIp, peerPort), TIMEOUT_MS);
                return socket;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });

        // Accept incoming connection
        Socket peerSocket = null;
        try {
            peerSocket = listener.accept();
        } catch (SocketTimeoutException ignored) {}

        // If accept failed, use outgoing connection
        if (peerSocket == null) {
            peerSocket = outgoingFuture.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } else {
            // Cancel outgoing connection if accept succeeded
            outgoingFuture.cancel(true);
        }

        listener.close();
        serverSocket.close();

        if (peerSocket != null) {
            System.out.println("✅ Connected to peer: " + peerSocket.getRemoteSocketAddress());

            BufferedReader peerIn = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()));
            PrintWriter peerOut = new PrintWriter(peerSocket.getOutputStream(), true);

            // Read incoming messages from peer
            executor.submit(() -> {
                try {
                    String line;
                    while ((line = peerIn.readLine()) != null) {
                        System.out.println("[Peer]: " + line);
                        System.out.print("You: ");
                    }
                } catch (IOException e) {
                    System.out.println("Peer disconnected.");
                }
            });

            // Send messages to peer
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            String msg;
            while ((msg = console.readLine()) != null) {
                peerOut.println(msg);
            }

            peerSocket.close();
        } else {
            System.err.println("❌ Failed to connect to peer.");
        }

        executor.shutdownNow();
    }
}
