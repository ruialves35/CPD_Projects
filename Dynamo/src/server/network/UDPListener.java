package server.network;

import common.Message;
import common.Utils;
import server.cluster.MembershipService;
import server.cluster.Node;
import server.storage.StorageService;
import server.storage.TransferService;
import java.io.*;
import java.net.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

public class UDPListener implements Runnable {
    private final StorageService storageService;
    private final MembershipService membershipService;
    private final TransferService transferService;
    private final ExecutorService executorService;

    private final HashSet<String> repliedNodes;

    public UDPListener(StorageService storageService, MembershipService membershipService, TransferService transferService,
                       ExecutorService executorService) {
        this.storageService = storageService;
        this.membershipService = membershipService;
        this.transferService = transferService;
        this.executorService = executorService;
        this.repliedNodes = new HashSet<>();
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

                if (!this.processEvent(packet)) // TODO: RUN IN ANOTHER THREAD
                    break;
            }

            socket.leaveGroup(group, netInf);
            socket.close();
        } catch (IOException e) {
            System.out.println("Error opening UDP server");
            throw new RuntimeException(e);
        }
    }

    private boolean processEvent(DatagramPacket packet) {
        //System.out.println("Got Packet from :" + packet.getAddress());
        // TODO Parse message and generate event
        Message message = new Message(packet.getData());

        InputStream is = new ByteArrayInputStream(message.getBody());
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String nodeId;
        int tcpPort, membershipCounter;
        try {
            nodeId = br.readLine();
            tcpPort = Integer.parseInt(br.readLine());
            membershipCounter = Integer.parseInt(br.readLine());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.printf("Received message from: %s (port %d). Membership Counter: %d%n", nodeId, tcpPort, membershipCounter);
        if (this.repliedNodes.contains(Utils.generateKey(nodeId))) {
            System.out.println("Received join from node that was already replied.");
            return true;
        }

        // Updates view of the cluster membership and adds the log
        this.membershipService.getNodeMap().put(Utils.generateKey(nodeId), new Node(nodeId, tcpPort));
        this.membershipService.addLog(nodeId, membershipCounter);

        // Updated membership info TODO: CHECK IF THIS IS OK
        this.repliedNodes.clear();
        this.repliedNodes.add(Utils.generateKey(nodeId));

        final int randomWait = new Random().nextInt(Utils.maxResponseTime);
        try {
            Thread.sleep(randomWait * 1000);

            final byte[] body = this.membershipService.buildMembershipMsgBody();
            Message msg = new Message("reply", "join", body);
            Sender.sendTCPMessage(msg.toBytes(), nodeId, tcpPort);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // TODO: send membership message

        return !"end".equals(message);
    }
}
