package server.network;

import common.Message;
import server.cluster.MembershipService;
import server.storage.StorageService;
import server.storage.TransferService;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.concurrent.ExecutorService;

public class UDPListener implements Runnable {
    private final StorageService storageService;
    private final MembershipService membershipService;
    private final TransferService transferService;
    private final ExecutorService executorService;

    public UDPListener(StorageService storageService, MembershipService membershipService,
            TransferService transferService,
            ExecutorService executorService) {
        this.storageService = storageService;
        this.membershipService = membershipService;
        this.transferService = transferService;
        this.executorService = executorService;
    }

    public void run() {
        try {
            MulticastSocket socket = new MulticastSocket(this.membershipService.getMulticastIPPort());
            InetSocketAddress group = new InetSocketAddress(
                    this.membershipService.getMulticastIpAddr(),
                    this.membershipService.getMulticastIPPort());
            NetworkInterface netInf = NetworkInterface.getByIndex(0);
            socket.joinGroup(group, netInf);

            System.out.println("Listening UDP messages");
            while (true) {
                byte[] msg = new byte[Message.MAX_MSG_SIZE];
                DatagramPacket packet = new DatagramPacket(msg, msg.length);

                socket.receive(packet);
                try {
                    final Message message = new Message(packet.getData());
                    executorService.submit(() -> processEvent(message));

                    if (message.getAction().equals("leave"))
                        break;
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }

            socket.leaveGroup(group, netInf);
            socket.close();
        } catch (IOException e) {
            System.out.println("Error opening UDP server");
            throw new RuntimeException(e);
        }
    }

    private void processEvent(Message message) {
        InputStream is = new ByteArrayInputStream(message.getBody());
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String nodeId;
        int tcpPort, membershipCounter;
        try {
            nodeId = br.readLine();
            tcpPort = Integer.parseInt(br.readLine());
            membershipCounter = Integer.parseInt(br.readLine());

            System.out.printf("Received message from: %s (port %d). Membership Counter: %d%n", nodeId, tcpPort,
                    membershipCounter);

            switch (message.getAction()) {
                case "join" -> this.membershipService.handleJoinRequest(nodeId, tcpPort, membershipCounter);
                case "leave" -> {
                    // Updates view of the cluster membership and adds the log
                    this.membershipService.removeNodeFromMap(nodeId);
                    this.membershipService.addLog(nodeId, membershipCounter);
                }
            }
        } catch (IOException e) {
            System.out.println("Error processing UDP event: " + message.getAction());
        }
    }
}
