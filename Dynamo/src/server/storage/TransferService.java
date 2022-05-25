package server.storage;

import common.Message;
import common.Sender;
import common.Utils;
import server.cluster.Node;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class TransferService {
    private final TreeMap<String, Node> nodeMap;

    public TransferService(TreeMap<String, Node> nodeMap) {
        this.nodeMap = nodeMap;
    }

    public boolean join(Node node) {
        String key = Utils.generateKey(node.getId());
        Node nextNode = this.getNextNode(key);
        String nextKey = Utils.generateKey(nextNode.getId());
        File[] nodeFiles = this.getNodeFiles(nextKey);

        System.out.println("Node: " + node);
        System.out.println("Node key: " + key);
        System.out.println("Next node: " + nextNode.getId());
        System.out.println("Next key: " + nextKey);
        if (sendFiles(nodeFiles, node, nextNode, true)) {
            System.out.println("Sent nodes to joined node successfully");
        } else {
            System.out.println("Error sending files to joined node");
        }

        return true;
    }

    public void leave(Node node) {
        // TODO When a node leaves, it must put the files on next node
        // TODO Change this to get the key from the node received by argument (it's just for tests)

        String key = Utils.generateKey(node.getId());
        File[] nodeFiles = this.getNodeFiles(key);

        Node nextNode = this.getNextNode(key);

        if (sendFiles(nodeFiles, node, nextNode, false)) {
            System.out.println("Sent nodes from the leaving node successfully");
        } else {
            System.out.println("Error sending files from leaving node to next node");
        }
    }


    /**
     * Creates a Message request to save a file
     * @param file file to be saved
     * @return if everything went well Message with saveFile actio,
     *         otherwise Message with error action
     */
    public Message createMsgFromFile(File file) {
        try (FileInputStream fis = new FileInputStream(file.getPath())) {
            byte[] value = fis.readAllBytes();
            return new Message("req", "saveFile", value);
        } catch (IOException e) {
            System.out.println("Error opening file in get operation: " + file.getPath());
            return new Message("REP", "error", null);
        }
    }


    /**
     * Sends all the files in a node Database by sending a TCP message.
     * If isJoin, then sends files from nextNode to Node
     * otherwise, sends files from node to nextNode
     * @param nodeFiles files from the node
     * @param node node that's joining or leaving
     * @param nextNode nextNode to node in the membership
     * @param isJoin is join action
     * @return true if no errors occur, false otherwise
     */
    public boolean sendFiles(File[] nodeFiles, Node node, Node nextNode, boolean isJoin) {
        if (nodeFiles != null) {
            for (final File file : nodeFiles) {
                String fileName = file.getName();
                String fileHash = Utils.generateKey(fileName);
                String key = Utils.generateKey(node.getId());
                int compare = fileHash.compareTo(key);
                boolean compareCondition = isJoin ? compare < 0 : true;

                if (compareCondition) {
                    Message msg = createMsgFromFile(file);
                    try {
                        if (isJoin) {
                            System.out.println("Join Message:" + msg.getAction());
                            msg.toBytes();
                            //Sender.sendTCPMessage(msg.toBytes(), node.getId(), node.getPort());
                        } else {
                            System.out.println("Leave Message:" + msg.toString());
                            //Sender.sendTCPMessage(msg.toBytes(), nextNode.getId(), nextNode.getPort());
                        }
                    } catch (IOException e) {
                        System.out.println("Error creating message from file: " + fileName);
                        return false;
                    }
                }
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
     * gets the Files in Node's database
     * @param key key of the node
     * @return array with Files stores in Node's database
     */
    public File[] getNodeFiles(String key) {

        // if it's join then we want to get the files of the next node
        // if it's leave we want to get the files of the current node
        String folderPath = "database/" + key + "/";
        File folder = new File(folderPath);
        File[] nodeFiles = folder.listFiles();
        return nodeFiles;
    }

}
