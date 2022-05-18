package server.cluster;

import server.network.Message;
import server.Utils;
import server.network.Sender;

import java.nio.charset.StandardCharsets;
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
    }

    @Override
    public boolean join() {
        if (this.isRootNode) return true;

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
        Message msg = new Message("request", "get", "ola multicast".getBytes(StandardCharsets.UTF_8));
        Sender.sendMulticast(msg.toBytes(), this.multicastIpAddr, this.multicastIPPort);
    }

    public int getMulticastIPPort() {
        return multicastIPPort;
    }

    public String getMulticastIpAddr() {
        return multicastIpAddr;
    }
}

/**
 * Use the membership Log to recognize how many nodes are in the system currently. This way, if there are less than 3, we can join with less than
 * 3 joins. For the first node however, maybe we should initialize it through the optional argument.
 *
 * We need to establish the header
 */
