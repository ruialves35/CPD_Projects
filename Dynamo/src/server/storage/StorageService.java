package server.storage;

import common.Message;
import common.Utils;
import server.cluster.Node;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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

        String filePath = dbFolder + key;
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(value);
        } catch (IOException e) {
            System.out.println("Error opening file in put operation: " + filePath);
            return new Message("REP", "error", null);
        }

        return new Message("REP", "ok", null);
    }

    @Override
    public Message get(String key) {
        Node node = getResponsibleNode(key);
        if (!node.getId().equals(ownID))
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

        String filePath = dbFolder + key;
        File file = new File(filePath);
        if (!file.delete()) {
            System.out.println("Error deleting the file: " + filePath);
            return new Message("REP", "error", null);
        }

        return new Message("REP", "ok", null);
    }

    /**
     * This method ensures the binary search's O(log N) time complexity by using the
     * TreeMap.ceilingKey() method, which takes advantage of a Red-Black BST.
     */
    private Node getResponsibleNode(String key) {
        Map.Entry<String, Node> nodeEntry = nodeMap.ceilingEntry(key);

        // No node with greater key -> Go to the start of the circle (first node)
        if (nodeEntry == null) nodeEntry = nodeMap.firstEntry();

        return nodeEntry.getValue();
    }

    private String createNodeFolder() {
        String folderPath = "database/" + Utils.generateKey(ownID) + "/";
        File folder = new File(folderPath);

        if (!folder.mkdirs() && !folder.isDirectory()) {
            System.out.println("Error creating the node's folder: " + folderPath);
        }

        return folderPath;
    }

    private Message buildRedirectMessage(Node newNode) {
        String redirectInfo = newNode.getId() + "\r\n" + newNode.getPort();
        return new Message("REP", "redirect", redirectInfo.getBytes(StandardCharsets.UTF_8));
    }
}
