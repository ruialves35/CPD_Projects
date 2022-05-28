package server.storage;

import common.Message;
import common.Sender;
import common.Utils;
import server.cluster.Node;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TransferService {
    private final TreeMap<String, Node> nodeMap;
    private final StorageService storageService;
    public TransferService(TreeMap<String, Node> nodeMap, StorageService storageService) {
        this.nodeMap = nodeMap;
        this.storageService = storageService;
    }

    public boolean join(Node node) {
        String key = Utils.generateKey(node.getId());
        Node nextNode = this.getNextNode(key);
        String nextKey = Utils.generateKey(nextNode.getId());
        ArrayList<String> nextNodeFiles;

        try {
            nextNodeFiles = this.getNodeFilesNames(nextNode);
        } catch (IOException e) {
            System.out.println("Could not get the files from the next node on Join.");
            throw new RuntimeException(e);
        }

        if (processJoin(nextNodeFiles, node, nextNode)) {
            System.out.println("Sent files from " + nextNode.getId() + " to " + node.getId());
        } else {
            System.out.println("Error sending files to joined node");
            return false;
        }

        return true;
    }

    public void leave(Node node) {
        String key = Utils.generateKey(node.getId());

        String folderPath = "database/" + key + "/";
        File folder = new File(folderPath);
        File[] nodeFiles = folder.listFiles();

        Node nextNode = this.getNextNode(key);

        if (nodeFiles == null){
            System.out.println("Node had no files to send on leave");
        } else if (sendNodeFiles(nodeFiles, nextNode)) {
            System.out.println("Sent nodes from the leaving node successfully");
        } else {
            System.out.println("Error sending files from leaving node to next node");
        }
    }


    /**
     * Creates a Message request to save a file
     * @param file file to be saved
     * @return if everything went well Message with saveFile action,
     *         otherwise Message with error action
     */
    public Message createMsgFromFile(File file) {
        try (FileInputStream fis = new FileInputStream(file.getPath())) {

            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(file.getName().getBytes(StandardCharsets.UTF_8));
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
            out.write(fis.readAllBytes());

            return new Message("REQ", "saveFile", out.toByteArray());
        } catch (IOException e) {
            System.out.println("Error opening file in get operation: " + file.getPath());
            throw new RuntimeException(e);
            //return new Message("REP", "error", null);
        }
    }


    /**
     * Processes a join by checking if next node has files from this node
     * if it has, then request to get and delete those files, and store
     * them in this node
     * @param nextNodeFiles array with the name of the files stored in nextNode
     * @param node node that's joining or leaving
     * @param nextNode nextNode to this node in the membership
     * @return true if no errors occur, false otherwise
     */
    public boolean processJoin(ArrayList<String> nextNodeFiles, Node node, Node nextNode) {
        if (nextNodeFiles.size() != 0) {
            for (final String fileName : nextNodeFiles) {
                try {
                    String key = Utils.generateKey(node.getId());
                    String nextNodeKey = Utils.generateKey(nextNode.getId());

                    // file possibly belongs to the node
                    if (fileName.compareTo(key) <= 0) {

                        if (key.compareTo(nextNodeKey) < 0 || fileName.compareTo(nextNodeKey) > 0) {
                            Message requestFile = new Message("REQ", "getAndDelete", fileName.getBytes(StandardCharsets.UTF_8));
                            byte[] response = Sender.sendTCPMessage(requestFile.toBytes(), nextNode.getId(), nextNode.getPort());
                            Message responseMsg = new Message(response);
                            storageService.saveFile(key, responseMsg.getBody());
                        }

                    }

                } catch (IOException e) {
                    System.out.println("Could not get the files from the next node on Join.");
                    throw new RuntimeException(e);
                }
            }
        }
        return true;
    }


    /**
     * Sends the files in nodeFiles to a node
     * @param nodeFiles array with the files to send
     * @param node node to which we want to send the files
     * @return true if sent all files, false otherwise
     */
    public boolean sendNodeFiles(File[] nodeFiles, Node node) {
        for (final File file : nodeFiles) {
            Message msg = createMsgFromFile(file);
            try {
                Sender.sendTCPMessage(msg.toBytes(), node.getId(), node.getPort());
            } catch (IOException e) {
                System.out.println("Could not send TCP message to send files to node " + node.getId());
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    /**
     *  Gets next node to node with key. If there is no node with higher key value,
     *  then gets the first node, so it behaves like a circular map
     * @param key key of the node that we want to get the next node
     * @return Next Node
     */
    public Node getNextNode(String key) {
        // Get the next node to store the files
        Map.Entry<String, Node> nextEntry = nodeMap.higherEntry(key);
        if (nextEntry == null) nextEntry = nodeMap.firstEntry();

        Node nextNode = nextEntry.getValue();

        return nextNode;
    }


    /**
     * gets the name of the files in a node by requesting it throw TCP
     * @param node node from which we want to get the files' names
     * @return array with the name of the files stored in node's database
     */
    public ArrayList<String> getNodeFilesNames(Node node) throws IOException {
        Message message = new Message("REQ", "getFiles", Utils.generateKey(node.getId()).getBytes(StandardCharsets.UTF_8));

        byte[] response = Sender.sendTCPMessage(message.toBytes(), node.getId(), node.getPort());

        Message reply = new Message(response);

        final BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(reply.getBody())));

        ArrayList<String> lines = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }

        return lines;
    }

}
