package server.cluster;

import server.Utils;

import java.io.IOException;
import java.net.*;
import java.util.SortedMap;
import java.util.TreeMap;

public class MembershipService implements ClusterMembership {
    private final SortedMap<String, Node> nodeMap;
    private final String multicastIpAddr;
    private final int multicastIPPort;
    private final String nodeId;
    private final boolean isRootNode;
    private int membershipCounter;  // NEEDS TO BE STORED IN NON-VOLATILE MEMORY
    private static final int maxRetransmissions = 3;

    public MembershipService(String multicastIPAddr, int multicastIPPort, String nodeId, boolean isRootNode) {
        nodeMap = new TreeMap<>();
        this.multicastIpAddr = multicastIPAddr;
        this.multicastIPPort = multicastIPPort;
        this.nodeId = nodeId;
        this.isRootNode = isRootNode;

        if (isRootNode) this.listen();
        else this.join();
    }

    @Override
    public boolean join() {
        // TODO Join protocol
        this.multicastJoin();

        Node newNode = new Node("temp");
        String key = Utils.generateKey("temp");
        nodeMap.put(key, newNode);

        return true;
    }

    @Override
    public void leave() {
        // TODO Leave protocol
        if (nodeMap.size() > 0) nodeMap.remove(Utils.generateKey("temp"));
    }

    public SortedMap<String, Node> getNodeMap() {
        return nodeMap;
    }

    private void multicastJoin() {
        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress group = InetAddress.getByName(this.multicastIpAddr);

            String message = "ola udp";
            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length, group, this.multicastIPPort);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listen() {
        try {
            MulticastSocket socket = new MulticastSocket(this.multicastIPPort);
            InetSocketAddress group = new InetSocketAddress(this.multicastIpAddr, this.multicastIPPort);

            // TODO: SHOULD WE USE THE 1ST INTERFACE? NOT SURE IF THERE IS ANOTHER WAY
            NetworkInterface netInf = NetworkInterface.getByIndex(0);
            socket.joinGroup(group, netInf);

            System.out.println("Listening for memberships...");
            while (true) {
                String message = "ola udp";
                DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length);
                socket.receive(packet);

                String received = new String(
                        packet.getData(), 0, packet.getLength());
                System.out.println("Received packet: " + received);
                if ("end".equals(received)) break;
            }

            socket.leaveGroup(group, netInf);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

/**
 * Use the membership Log to recognize how many nodes are in the system currently. This way, if there are less than 3, we can join with less than
 * 3 joins. For the first node however, maybe we should initialize it through the optional argument.
 *
 * We need to establish the header
 */
