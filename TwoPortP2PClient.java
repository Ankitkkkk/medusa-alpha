import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class TwoPortP2PClient {
    private static final int TIMEOUT_MS = 10000;
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static void main(String[] args) throws Exception {
        String serverIp = "13.204.76.236";
        int serverPort = 80;

        // Two ports: one for accepting, one for sending (outgoing)
        int listenPort = 50000 + (int)(Math.random() * 1000);
        int sendPort = 55000 + (int)(Math.random() * 1000); // use a different port to avoid bind conflict

        // Connect to signaling server
        Socket serverSocket = new Socket(serverIp, serverPort);
        BufferedReader serverIn = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
        PrintWriter serverOut = new PrintWriter(serverSocket.getOutputStream(), true);

        // Send only the listening port
        serverOut.println("HELLO:" + listenPort);

        String response = serverIn.readLine();
        if (response == null || !response.startsWith("PEER:")) {
            System.err.println("Invalid server response");
            serverSocket.close();
            return;
        }

        // Parse peer info
        String[] parts = response.split(":");
        String peerIp = parts[1];
        int peerListenPort = Integer.parseInt(parts[2]);
        System.out.println("Got peer info: " + peerIp + ":" + peerListenPort);

        serverSocket.close();

        // STEP 1: Start listener on listenPort (for incoming peer connection)
        ServerSocket listener = new ServerSocket(listenPort);
        listener.setSoTimeout(TIMEOUT_MS);

        // STEP 2: Try to connect to peer using a different port (sendPort)
        Future<Socket> outgoingFuture = executor.submit(() -> {
            try {
                Thread.sleep(500); // let listener start
                Socket socket = new Socket();
                socket.bind(new InetSocketAddress(sendPort)); // use a different port
                socket.connect(new InetSocketAddress(peerIp, peerListenPort), TIMEOUT_MS);
                return socket;
            } catch (IOException e) {
                System.out.println("IOExceptions: ");
                e.printStackTrace();
                return null;
            }
        });

        // STEP 3: Accept if peer connects to us first
        Socket peerSocket = null;
        try {
            peerSocket = listener.accept();
        } catch (SocketTimeoutException ignored) {
            System.out.println("listenr error: ");
            ignored.printStackTrace();
        }

        if (peerSocket == null) {
            peerSocket = outgoingFuture.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } else {
            outgoingFuture.cancel(true);
        }

        listener.close();

        if (peerSocket != null) {
            System.out.println("✅ Connected to peer: " + peerSocket.getRemoteSocketAddress());

            BufferedReader peerIn = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()));
            PrintWriter peerOut = new PrintWriter(peerSocket.getOutputStream(), true);

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
