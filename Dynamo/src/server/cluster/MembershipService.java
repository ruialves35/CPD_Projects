package server.cluster;

import common.Message;
import common.Sender;
import common.Utils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.TreeMap;

public class MembershipService implements ClusterMembership {
    private final TreeMap<String, Node> nodeMap;
    private final String multicastIpAddr;
    private final int multicastIPPort;
    private final String nodeId;
    private final boolean isRootNode;
    private final String folderPath;
    private int membershipCounter;  // NEEDS TO BE STORED IN NON-VOLATILE MEMORY
    private static final int maxRetransmissions = 3;

    public MembershipService(String multicastIPAddr, int multicastIPPort, String nodeId, boolean isRootNode) {
        nodeMap = new TreeMap<>();
        this.multicastIpAddr = multicastIPAddr;
        this.multicastIPPort = multicastIPPort;
        this.nodeId = nodeId;
        this.isRootNode = isRootNode;
        this.folderPath = Utils.generateFolderPath(nodeId);
        this.createNodeFolder();
        // this.createMembershipLog();
    }

    @Override
    public boolean join() {
        // TODO Join protocol
        if (!this.isRootNode) this.multicastJoin();

        Node newNode = new Node("127.0.0.1", 3000);
        String key = Utils.generateKey("127.0.0.1");
        nodeMap.put(key, newNode);

        Node newNode2 = new Node("127.0.0.2", 3000);
        String key2 = Utils.generateKey("127.0.0.2");
        nodeMap.put(key2, newNode2);

        return true;
    }

    @Override
    public void leave() {
        // TODO Leave protocol
        if (nodeMap.size() > 0) nodeMap.remove(Utils.generateKey("temp"));
    }

    public TreeMap<String, Node> getNodeMap() {
        return nodeMap;
    }

    private void multicastJoin() {
        ByteBuffer body = ByteBuffer.allocate(4);
        body.putInt(this.membershipCounter);
        Message msg = new Message("request", "join", body.array());
        Sender.sendMulticast(msg.toBytes(), this.multicastIpAddr, this.multicastIPPort);
    }

    public int getMulticastIPPort() {
        return multicastIPPort;
    }

    public String getMulticastIpAddr() {
        return multicastIpAddr;
    }

    private void createNodeFolder() {
        File folder = new File(this.folderPath);

        if (!folder.mkdirs() && !folder.isDirectory()) {
            System.out.println("Error creating the node's folder: " + this.folderPath);
        } else {
            this.createMembershipLog();
        }
    }

    private void createMembershipLog() {
        File memberLog = new File(this.folderPath + "membership.log");
        try {
            if(!memberLog.createNewFile()) {
                PrintWriter writer = new PrintWriter(memberLog);
                writer.print("");
                writer.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

/**
 * Use the membership Log to recognize how many nodes are in the system currently. This way, if there are less than 3, we can join with less than
 * 3 joins. For the first node however, maybe we should initialize it through the optional argument.
 *
 * We need to establish the header
 */
