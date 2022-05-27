package server.cluster;

import common.Message;
import common.Sender;
import common.Utils;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class MembershipService implements ClusterMembership {
    private final TreeMap<String, Node> nodeMap;
    private final String multicastIpAddr;
    private final int multicastIPPort;
    private final String nodeId;

    private final int tcpPort;
    private final String folderPath;
    private int membershipCounter = 0;  // NEEDS TO BE STORED IN NON-VOLATILE MEMORY TO SURVIVE NODE CRASHES
    private static final int maxRetransmissions = 3;
    private int retransmissionCounter = 0;
    private final HashSet<String> membershipReplyNodes;

    public MembershipService(String multicastIPAddr, int multicastIPPort, String nodeId, int tcpPort) {
        nodeMap = new TreeMap<>();
        this.multicastIpAddr = multicastIPAddr;
        this.multicastIPPort = multicastIPPort;
        this.nodeId = nodeId;
        this.tcpPort = tcpPort;
        this.folderPath = Utils.generateFolderPath(nodeId);
        this.createNodeFolder();
        this.membershipReplyNodes = new HashSet<>();
    }

    @Override
    public boolean join() {
        if (this.membershipCounter == 0 || !isClusterMember(this.membershipCounter)) {
            if (this.membershipCounter != 0)    // Edge case for the first join
                this.updateMembershipCounter(this.membershipCounter + 1);
            this.addLog(this.nodeId, this.membershipCounter);
            this.multicastJoin();
            // TODO: OPEN UDP / TCP LISTENER ??
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean leave() {
        // TODO Leave protocol
        if (isClusterMember(this.membershipCounter)) {
            this.removeNodeFromMap(this.nodeId);
            this.updateMembershipCounter(this.membershipCounter + 1);
            this.addLog(this.nodeId, this.membershipCounter);
            this.multicastLeave();
            // TODO: CLOSE UDP / TCP LISTENER ??
            return true;
        } else {
            return false;
        }
    }

    /**
     * Updates the membershipCounter value and stores it in non-volatile memory
     * @param newCounter
     */
    public void updateMembershipCounter(int newCounter) {
        this.membershipCounter = newCounter;

        try {
            File memberCounter = new File(this.folderPath + Utils.membershipCounterFileName);
            FileWriter counterWriter;
            counterWriter = new FileWriter(memberCounter, false);
            counterWriter.write(String.valueOf(newCounter));
            counterWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public TreeMap<String, Node> getNodeMap() {
        return nodeMap;
    }

    private void multicastJoin() {
        byte[] joinBody = buildMembershipBody();
        Message msg = new Message("request", "join", joinBody);

        while (this.retransmissionCounter < maxRetransmissions) {
            try {
                Sender.sendMulticast(msg.toBytes(), this.multicastIpAddr, this.multicastIPPort);
                Thread.sleep(Utils.timeoutTime);
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }

            if (this.membershipReplyNodes.size() >= Utils.numMembershipMessages) {
                this.membershipReplyNodes.clear();
                this.retransmissionCounter = 0;
                System.out.println("New Node joined the distributed store");
                return;
            }

            this.retransmissionCounter++;
        }

        System.out.println("Prime Node joined the distributed store.");
    }

    private void multicastLeave() {
        byte[] joinBody = buildMembershipBody();
        Message msg = new Message("request", "leave", joinBody);

        try {
            Sender.sendMulticast(msg.toBytes(), this.multicastIpAddr, this.multicastIPPort);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Node left the distributed store.");
    }

    /**
     * Body has nodeId, tcp port and membership Counter
     */
    private byte[] buildMembershipBody() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.nodeId).append(Utils.newLine);
        sb.append(this.tcpPort).append(Utils.newLine);
        sb.append(this.membershipCounter).append(Utils.newLine);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out.toByteArray();
    }

    /**
     *
     * @param counter
     * @return true if a node is a member of the nodeMap. False otherwise.
     */
    public static boolean isClusterMember(int counter) {
        return counter % 2 == 0;
    }

    public int getMulticastIPPort() {
        return multicastIPPort;
    }

    public String getMulticastIpAddr() {
        return multicastIpAddr;
    }

    public String getNodeId() {return nodeId;}

    public int getTcpPort() {return tcpPort;}

    /**
     * Adds a new node to the nodeMap.
     * @param newNodeId
     * @param newNodePort
     */
    public void addNodeToMap(String newNodeId, int newNodePort) {
        String key = Utils.generateKey(newNodeId);
        if (!this.nodeMap.containsKey(key)) {
            Node newNode = new Node(newNodeId, newNodePort);
            this.nodeMap.put(key, newNode);
        }
    }

    /***
     * Removes a node from the map with a specific id
     * @param oldNodeId nodeId
     */
    public void removeNodeFromMap(String oldNodeId) {
        String key = Utils.generateKey(oldNodeId);
        this.nodeMap.remove(key);
    }

    private void createNodeFolder() {
        File folder = new File(this.folderPath);

        if (!folder.mkdirs() && !folder.isDirectory()) {
            System.out.println("Error creating the node's folder: " + this.folderPath);
        } else {
            // The membership log should be updated with the one received by other nodes already belonging to the system
            this.initializeNodeFiles();
        }
    }

    private void initializeNodeFiles() {
        try {
            File memberCounter = new File(this.folderPath + Utils.membershipCounterFileName);

            boolean foundCounter = false;
            if (!memberCounter.createNewFile()) {
                // Recover membership counter
                Scanner counterScanner = new Scanner(memberCounter);
                if (counterScanner.hasNextInt()) {
                    int counter = counterScanner.nextInt();
                    this.membershipCounter = counter;
                    foundCounter = true;
                }

                counterScanner.close();
            }
            if (!foundCounter) {
                // Create file and store membership counter
                FileWriter counterWriter = new FileWriter(memberCounter, false);
                counterWriter.write(String.valueOf(this.membershipCounter));
                counterWriter.close();
            }


            File memberLog = new File(this.folderPath + Utils.membershipLogFileName);

            // Set initial log to be the current node
            FileWriter writer = new FileWriter(memberLog, false);
            writer.write(String.format("%s %d", this.nodeId, this.membershipCounter));
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds a new log to the beginning of the membership log removing existing logs for that nodeId
     */
    public void addLog(String newNodeId, int newMemberCounter) {
        try {
            File file = new File(this.folderPath + Utils.membershipLogFileName);
            boolean isDeprecatedLog = false;

            FileReader fr = new FileReader(file);   //reads the file
            BufferedReader br = new BufferedReader(fr);
            String line;

            // Check if there is a more recent log for this node
            while ((line = br.readLine()) != null) {
                String[] lineData = line.split(" ");
                String lineId = lineData[0];
                if (newNodeId.equals(lineId)) {
                    int lineCounter = Integer.parseInt(lineData[1]);
                    if (lineCounter >= newMemberCounter)
                        isDeprecatedLog = true;
                    break;
                }
            }

            if (!isDeprecatedLog) {
                List<String> filteredFile = new ArrayList<>();

                filteredFile.add(String.format("%s %d", newNodeId, newMemberCounter));
                filteredFile.addAll(
                        Files.lines(file.toPath()).filter(streamLine -> {
                            Optional<String> optId = Arrays.stream(streamLine.split(" ")).findFirst();
                            String rowId = optId.orElse("");
                            return !newNodeId.equals(rowId);
                        }).toList());

                Files.write(file.toPath(), filteredFile, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

                // If the newMemberCounter is even, it is already added by the nodeMap received
                if (!isClusterMember(newMemberCounter)) {
                    // Remove the node from the nodeMap
                    this.removeNodeFromMap(newNodeId);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] buildMembershipMsgBody() {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

        try {
            // Write node id
            byteOut.write(this.nodeId.getBytes(StandardCharsets.UTF_8));
            byteOut.write(Utils.newLine.getBytes(StandardCharsets.UTF_8));

            File file = new File(this.folderPath + Utils.membershipLogFileName);
            Scanner myReader = new Scanner(file);
            for (int i = 0; i < Utils.numLogEvents; i++) {
                if (!myReader.hasNextLine()) break;
                String line = myReader.nextLine();
                byteOut.write(line.getBytes(StandardCharsets.UTF_8));
                byteOut.write(Utils.newLine.getBytes(StandardCharsets.UTF_8));
            }
            myReader.close();

            byteOut.write(Utils.newLine.getBytes(StandardCharsets.UTF_8));

            for (Node node : this.getNodeMap().values()) {
                String entryLine = node.getId() + " " + node.getPort() + "\n";
                byteOut.write(entryLine.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteOut.toByteArray();
    }

    public int getRetransmissionCounter() {
        return retransmissionCounter;
    }

    public HashSet<String> getMembershipReplyNodes() {
        return membershipReplyNodes;
    }
}

/**
 * Use the membership Log to recognize how many nodes are in the system currently. This way, if there are less than 3, we can join with less than
 * 3 joins. For the first node however, maybe we should initialize it through the optional argument.
 *
 * We need to establish the header
 */
