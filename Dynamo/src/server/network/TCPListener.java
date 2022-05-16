package server.network;

import server.cluster.MembershipService;
import server.storage.StorageService;
import server.storage.TransferService;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

public class TCPListener implements Runnable {
    private final StorageService storageService;
    private final MembershipService membershipService;
    private final TransferService transferService;
    private final ExecutorService executorService;
    private final int port;

    public TCPListener(StorageService storageService, MembershipService membershipService, TransferService transferService,
                       ExecutorService executorService, int port) {
        this.storageService = storageService;
        this.membershipService = membershipService;
        this.transferService = transferService;
        this.executorService = executorService;
        this.port = port;
    }

    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(this.port);
            System.out.println("Listening for TCP Messages...");
            while (true) {
                Socket socket = serverSocket.accept();
                DataInputStream stream = new DataInputStream(socket.getInputStream());

                String message = stream.readUTF();
                processEvent(message);
                System.out.println(message);
                if (message.equals("leave")) break;
            }
            serverSocket.close();
            }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processEvent(String message) {
        // TODO Parse message and generate event
    }
}
