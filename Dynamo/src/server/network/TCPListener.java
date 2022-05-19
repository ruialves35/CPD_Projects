package server.network;

import common.Message;
import common.Sender;
import server.cluster.MembershipService;
import server.storage.StorageService;
import server.storage.TransferService;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

public class TCPListener implements Runnable {
    private final StorageService storageService;
    private final MembershipService membershipService;
    private final TransferService transferService;
    private final ExecutorService executorService;
    private final String nodeIp;
    private final int port;

    public TCPListener(StorageService storageService, MembershipService membershipService, TransferService transferService,
                       ExecutorService executorService, String nodeIp, int port ) {
        this.storageService = storageService;
        this.membershipService = membershipService;
        this.transferService = transferService;
        this.executorService = executorService;
        this.nodeIp = nodeIp;
        this.port = port;
    }

    public void run() {
        try {
            InetAddress addr = InetAddress.getByName(nodeIp);
            ServerSocket serverSocket = new ServerSocket(this.port, 50, addr);
            System.out.println("Listening for TCP Messages in address " + serverSocket.getInetAddress() +
                    " port " + serverSocket.getLocalPort() + " ...");
            while (true) {
                Socket socket = serverSocket.accept();
                DataInputStream stream = new DataInputStream(socket.getInputStream());

                String replyIp = socket.getInetAddress().getHostAddress();
                int replyPort = socket.getPort();

                Message message = new Message(stream.readAllBytes());
                executorService.submit(() -> {
                    try {
                        processEvent(message, replyIp, replyPort);
                    } catch (IOException e) {
                        System.out.println("Error processing event");
                    }
                });

                if (message.getAction().equals("leave")) break;
            }
            serverSocket.close();
            }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processEvent(Message message, String replyIp, int replyPort) throws IOException {
        final ByteArrayInputStream stream = new ByteArrayInputStream(message.getBody());
        final BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(message.getBody())));

        switch (message.getAction()) {
            case "get" -> {
                // TODO Normal get -> send to te IP which requested
                byte[] file = storageService.get(new String(message.getBody()));
                Message reply = new Message("REP", "get", file);
                Sender.sendTCPMessage(reply.toBytes(), replyIp, replyPort);
            }
            case "forwardGet" -> {
                // TODO Forward -> Get the IP from the body ??
            }
            case "put" -> {
                String key = reader.readLine();
                int offset = key.length() + 2; // 2 = \r\n

                //noinspection ResultOfMethodCallIgnored
                stream.skip(offset);
                byte[] file = stream.readAllBytes();
                storageService.put(key, file);
            }
            case "delete" -> storageService.delete(new String(message.getBody()));
        }
    }
}
