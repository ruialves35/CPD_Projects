package server.storage;

import common.Message;
import common.Sender;
import common.Utils;
import server.Constants;
import server.cluster.Node;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

public class StorageService implements KeyValue {
    private final TreeMap<String, Node> nodeMap;
    private final String ownID;
    private final String dbFolder;

    public StorageService(TreeMap<String, Node> nodeMap, String ownID) {
        this.nodeMap = nodeMap;
        this.ownID = ownID;
        this.dbFolder = Utils.generateFolderPath(ownID);
    }

    @Override
    public Message put(String key, byte[] value) {
        Node node = getResponsibleNode(key);
        if (!node.getId().equals(ownID))
            return buildRedirectMessage(node);

        if (hasFile(key)) return new Message("REP", "ok", null);

        String filePath = dbFolder + key;
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(value);
        } catch (IOException e) {
            System.out.println("Error opening file in put operation: " + filePath);
            return new Message("REP", "error", null);
        }

        // Send the file to the following nodes (Replication)
        for (int i = 1; i < Constants.replicationFactor; ++i) {
            node = getNextNode(node);
            if (node.getId().equals(ownID)) break; // Not enough nodes available

            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                out.write(key.getBytes(StandardCharsets.UTF_8));
                out.write(Utils.newLine.getBytes(StandardCharsets.UTF_8));
                out.write(value);

                Message msg = new Message("REQ", "saveFile", out.toByteArray());

                // TODO What to do in case of error reply? What if there's no reply (crash)?
                // TODO It's stuck waiting for a response. Use thread pool?
                Sender.sendTCPMessage(msg.toBytes(), node.getId(), node.getPort());
            } catch (IOException e) {
                System.out.println("Error sending saveFile message to " + node.getId());
                e.printStackTrace();
            }
        }

        return new Message("REP", "ok", null);
    }

    @Override
    public Message get(String key) {

        Node node = getResponsibleNode(key);
        // The node can have the file due to replication
        if (!node.getId().equals(ownID) && !hasFile(key))
            return buildRedirectMessage(node);

        String filePath = dbFolder + key;
        byte[] value;

        try (FileInputStream fis = new FileInputStream(filePath)) {
            value = fis.readAllBytes();
        } catch (IOException e) {
            System.out.println("Error opening file in get operation: " + filePath);
            return new Message("REP", "error", null);
        }

        return new Message("REP", "ok", value);
    }

    @Override
    public Message delete(String key) {
        Node node = getResponsibleNode(key);
        if (!node.getId().equals(ownID))
            return buildRedirectMessage(node);

        // TODO Tombstones
        if (!hasFile(key)) return new Message("REP", "ok", null);

        String filePath = dbFolder + key;
        File file = new File(filePath);
        if (!file.delete()) {
            System.out.println("Error deleting the file: " + filePath);
            return new Message("REP", "error", null);
        }

        // Tell the following nodes to delete the file (Replication)
        for (int i = 1; i < Constants.replicationFactor; ++i) {
            node = getNextNode(node);
            if (node.getId().equals(ownID)) break; // Not enough nodes available

            try {
                Message msg = new Message("REQ", "getAndDelete", key.getBytes(StandardCharsets.UTF_8));

                // TODO What to do in case of error reply? What if there's no reply (crash)?
                // TODO It's stuck waiting for a response. Use thread pool?
                Sender.sendTCPMessage(msg.toBytes(), node.getId(), node.getPort());
            } catch (IOException e) {
                System.out.println("Error sending deleteFile message to " + node.getId());
                e.printStackTrace();
            }
        }

        return new Message("REP", "ok", null);
    }

    public Message getAndDelete(String key) {
        String filePath = dbFolder + key;
        byte[] value;

        try (FileInputStream fis = new FileInputStream(filePath)) {
            value = fis.readAllBytes();
        } catch (IOException e) {
            System.out.println("Error opening file in get operation: " + filePath);
            return new Message("REP", "error", null);
        }

        File file = new File(filePath);
        if (file.delete()) {
            System.out.println("Successfully Deleted file: " + file.getName() + " from node: " + dbFolder );
        } else {
            System.out.println("Failed to delete the file.");
        }

        return new Message("REP", "ok", value);
    }

    public Message saveFile(String key, byte[] file) {

        String filePath = dbFolder + key;
        if (hasFile(key)) return new Message("REP", "ok", null);

        Message reply;
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(file);
            reply = new Message("REP", "ok", null);

        } catch (IOException e) {
            System.out.println("Error opening file in put operation: " + filePath);
            reply = new Message("REP", "error", null);
        }

        return reply;
    }

    public String[] getFiles() {
        File folder = new File(dbFolder);
        return folder.list();
    }

    public String getDbFolder() {
        return dbFolder;
    }

    public int getNumberOfNodes() {
        return nodeMap.size();
    }

    public Node getNextNode(Node prevNode) {
        String prevKey = Utils.generateKey(prevNode.getId());
        Map.Entry<String, Node> nodeEntry = nodeMap.higherEntry(prevKey);

        // No node with greater key -> Go to the start of the circle (first node)
        if (nodeEntry == null) nodeEntry = nodeMap.firstEntry();

        return nodeEntry.getValue();
    }

    public Node getPreviousNode(Node node) {
        String key = Utils.generateKey(node.getId());
        Map.Entry<String, Node> nodeEntry = nodeMap.lowerEntry(key);

        // No node with lower key -> Go to the end of the circle (last node)
        if (nodeEntry == null) nodeEntry = nodeMap.lastEntry();

        return nodeEntry.getValue();
    }

    private boolean hasFile(String key) {
        String filePath = dbFolder + key;
        File file = new File(filePath);
        return file.exists();
    }

    /**
     * This method ensures the binary search's O(log N) time complexity by using the
     * TreeMap.ceilingKey() method, which takes advantage of a Red-Black BST.
     */
    public Node getResponsibleNode(String key) {
        Map.Entry<String, Node> nodeEntry = nodeMap.ceilingEntry(key);

        // No node with greater key -> Go to the start of the circle (first node)
        if (nodeEntry == null) nodeEntry = nodeMap.firstEntry();

        return nodeEntry.getValue();
    }

    private Message buildRedirectMessage(Node newNode) {
        String redirectInfo = newNode.getId() + Utils.newLine + newNode.getPort();
        return new Message("REP", "redirect", redirectInfo.getBytes(StandardCharsets.UTF_8));
    }
}
