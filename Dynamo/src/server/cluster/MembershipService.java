package server.cluster;

import common.Message;
import common.Sender;
import common.Utils;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public class MembershipService implements ClusterMembership {
    private final TreeMap<String, Node> nodeMap;
    private final String multicastIpAddr;
    private final int multicastIPPort;
    private final String nodeId;

    private final int tcpPort;
    private final boolean isRootNode;
    private final String folderPath;
    private int membershipCounter = 0;  // NEEDS TO BE STORED IN NON-VOLATILE MEMORY TO SURVIVE NODE CRASHES
    private static final int maxRetransmissions = 3;

    public MembershipService(String multicastIPAddr, int multicastIPPort, String nodeId, int tcpPort, boolean isRootNode) {
        nodeMap = new TreeMap<>();
        this.multicastIpAddr = multicastIPAddr;
        this.multicastIPPort = multicastIPPort;
        this.nodeId = nodeId;
        this.tcpPort = tcpPort;
        this.isRootNode = isRootNode;
        this.folderPath = Utils.generateFolderPath(nodeId);
        this.createNodeFolder();
    }

    @Override
    public boolean join() {
        Node newNode = new Node(this.nodeId, this.tcpPort);
        String key = Utils.generateKey(this.nodeId);
        nodeMap.put(key, newNode);

        if (this.isRootNode) return true;

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

    /**
     * Body has nodeId, tcp port and membership Counter
     */
    private void multicastJoin() {
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

        Message msg = new Message("request", "join", out.toByteArray());
        Sender.sendMulticast(msg.toBytes(), this.multicastIpAddr, this.multicastIPPort);
    }

    public int getMulticastIPPort() {
        return multicastIPPort;
    }

    public String getMulticastIpAddr() {
        return multicastIpAddr;
    }

    public String getNodeId() {return nodeId;}

    public int getTcpPort() {return tcpPort;}

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
    public void addLog(String nodeId, int membershipCounter) {
        try {
            File file = new File(this.folderPath + Utils.membershipLogFileName);

            List<String> filteredFile = new ArrayList<>();
            filteredFile.add(String.format("%s %d", nodeId, membershipCounter));
            filteredFile.addAll(
                Files.lines(file.toPath()).filter(line -> {
                    Optional<String> optId = Arrays.stream(line.split(" ")).findFirst();
                    String rowId = optId.orElse("");
                    return !nodeId.equals(rowId);
                }).collect(Collectors.toList()));

            Files.write(file.toPath(), filteredFile, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

            /*
            Scanner myReader = new Scanner(myObj);

            while (myReader.hasNextLine()) {
                Optional<String> optId = Arrays.stream(myReader.nextLine().split(" ")).findFirst();
                String rowId = optId.orElse("");
                if (nodeId.equals(rowId)) {

                }
            } */
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
            ByteArrayOutputStream byteOut2 = new ByteArrayOutputStream();
            ObjectOutputStream objectOut = new ObjectOutputStream(byteOut2);

            File file = new File(this.folderPath + Utils.membershipLogFileName);
            Scanner myReader = new Scanner(file);
            for (int i = 0; i < Utils.numLogEvents; i++) {
                if (!myReader.hasNextLine()) break;
                String line = myReader.nextLine();
                System.out.println("Writing line: " + line);
                byteOut.write(line.getBytes(StandardCharsets.UTF_8));
                byteOut.write(Utils.newLine.getBytes(StandardCharsets.UTF_8));
            }
            myReader.close();

            byteOut.write(Utils.newLine.getBytes(StandardCharsets.UTF_8));

            // Convert nodeMap to bytes
            objectOut.writeObject(this.getNodeMap());
            byteOut.writeBytes(byteOut2.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteOut.toByteArray();
    }
}

/**
 * Use the membership Log to recognize how many nodes are in the system currently. This way, if there are less than 3, we can join with less than
 * 3 joins. For the first node however, maybe we should initialize it through the optional argument.
 *
 * We need to establish the header
 */
