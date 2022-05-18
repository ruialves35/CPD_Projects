package server.network;

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

    public UDPListener(StorageService storageService, MembershipService membershipService, TransferService transferService,
                       ExecutorService executorService) {
        this.storageService = storageService;
        this.membershipService = membershipService;
        this.transferService = transferService;
        this.executorService = executorService;
    }

    public void run() {
        try {
            MulticastSocket socket = new MulticastSocket(this.membershipService.getMulticastIPPort());
            InetSocketAddress group = new InetSocketAddress(this.membershipService.getMulticastIpAddr(), this.membershipService.getMulticastIPPort());

            // TODO: SHOULD WE USE THE 1ST INTERFACE? NOT SURE IF THERE IS ANOTHER WAY
            NetworkInterface netInf = NetworkInterface.getByIndex(0);
            socket.joinGroup(group, netInf);

            System.out.println("Listening for memberships...");
            while (true) {
                byte[] msg = new byte[Message.MAX_MSG_SIZE];
                DatagramPacket packet = new DatagramPacket(msg, msg.length);

                socket.receive(packet);

                //System.out.println("Got Packet from :" + packet.getAddress());
                String received = new String(
                        packet.getData(), 0, packet.getLength());
                if (!this.processEvent(received))
                    break;
            }

            socket.leaveGroup(group, netInf);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean processEvent(String message) {
        // TODO Parse message and generate event
        System.out.println("Received packet: \n" + message);
        System.out.println("-----------------");

        return "end".equals(message);
    }
}
