package server.network;

import common.Message;
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
                    " port " + serverSocket.getLocalPort());
            while (true) {
                Socket socket = serverSocket.accept();
                DataInputStream istream = new DataInputStream(socket.getInputStream());
                DataOutputStream ostream = new DataOutputStream(socket.getOutputStream());

                Message message = new Message(istream.readAllBytes());
                executorService.submit(() -> {
                    try {
                        processEvent(message, ostream);
                        istream.close();
                        ostream.close();
                    } catch (IOException e) {
                        System.out.println("Error processing event");
                        // TODO Handle specific errors
                        Message errorMsg = new Message("REP", "error", null);
                        try {
                            ostream.write(errorMsg.toBytes());
                            istream.close();
                            ostream.close();
                        } catch (IOException ioException) {
                            System.out.println("Error sending error message");
                        }
                    }
                });

                if (message.getAction().equals("leave")) break;
            }
            serverSocket.close();
            }
        catch (IOException e) {
            System.out.println("Error opening TCP server");
            throw new RuntimeException(e);
        }
    }

    private void processEvent(Message message, DataOutputStream ostream) throws IOException {
        final ByteArrayInputStream stream = new ByteArrayInputStream(message.getBody());
        final BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(message.getBody())));
        Message reply;

        switch (message.getAction()) {
            case "get" -> reply = storageService.get(new String(message.getBody()));
            case "put" -> {
                String key = reader.readLine();
                int offset = key.length() + 2; // 2 = \r\n

                //noinspection ResultOfMethodCallIgnored
                stream.skip(offset);
                byte[] file = stream.readAllBytes();
                reply = storageService.put(key, file);
            }
            case "delete" -> reply = storageService.delete(new String(message.getBody()));
            default -> {
                System.out.println("Invalid event received!");
                return;
            }
        }

        ostream.write(reply.toBytes());
    }
}
