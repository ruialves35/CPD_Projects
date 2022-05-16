package server.network;

import server.Message;
import server.cluster.MembershipService;
import server.storage.StorageService;
import server.storage.TransferService;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;

public class UDPListener implements Runnable {
    private final StorageService storageService;
    private final MembershipService membershipService;
    private final TransferService transferService;
    private final ExecutorService executorService;
    private final int multicastIPPort;
    private final String multicastIpAddr;

    public UDPListener(StorageService storageService, MembershipService membershipService, TransferService transferService,
                       ExecutorService executorService, String multicastIpAddr, int multicastIPPort) {
        this.storageService = storageService;
        this.membershipService = membershipService;
        this.transferService = transferService;
        this.executorService = executorService;
        this.multicastIPPort = multicastIPPort;
        this.multicastIpAddr = multicastIpAddr;
    }

    public void run() {
        try {
            MulticastSocket socket = new MulticastSocket(this.multicastIPPort);
            InetSocketAddress group = new InetSocketAddress(this.multicastIpAddr, this.multicastIPPort);

            // TODO: SHOULD WE USE THE 1ST INTERFACE? NOT SURE IF THERE IS ANOTHER WAY
            NetworkInterface netInf = NetworkInterface.getByIndex(0);
            socket.joinGroup(group, netInf);

            System.out.println("Listening for UDP Messages...");
            while (true) {
                byte[] msg = new byte[Message.MAX_MSG_SIZE];
                DatagramPacket packet = new DatagramPacket(msg, msg.length);

                socket.receive(packet);

                String received = new String(
                        packet.getData(), 0, packet.getLength());
                if ("leave".equals(received)) break;
            }

            socket.leaveGroup(group, netInf);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processEvent(String message) {
        // TODO Parse message and generate event
    }
}
