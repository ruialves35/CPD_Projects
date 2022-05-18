package server.network;

import server.cluster.MembershipService;
import server.storage.StorageService;
import server.storage.TransferService;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

public class TCPListener implements Runnable {
    private final StorageService storageService;
    private final MembershipService membershipService;
    private final TransferService transferService;
    private final ExecutorService executorService;
    private String nodeIp;
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

                Message message = new Message(stream.readAllBytes());
                processEvent(message);
                if (message.getAction().equals("leave")) break;
            }
            serverSocket.close();
            }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processEvent(Message message) {
        // TODO Parse message and generate event
    }
}
