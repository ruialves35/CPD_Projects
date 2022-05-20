package server.storage;

import server.Utils;
import server.cluster.Node;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class StorageService implements KeyValue {
    private final TreeMap<String, Node> nodeMap;
    private final String ownID;
    private final String dbFolder;

    public StorageService(TreeMap<String, Node> nodeMap, String ownID) {
        this.nodeMap = nodeMap;
        this.ownID = ownID;
        this.dbFolder = createNodeFolder();
    }

    @Override
    public String put(byte[] value) {
        String key = Utils.generateKey(value);

        Node node = getResponsibleNode(key);
        if (!node.getId().equals(ownID)) {
            // TODO Request put to the node
            return null;
        }

        String filePath = dbFolder + key;
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(value);
        } catch (IOException e) {
            System.out.println("Error opening file in put operation: " + filePath);
            e.printStackTrace();
            return null;
        }

        return key;
    }

    @Override
    public byte[] get(String key) {
        Node node = getResponsibleNode(key);
        if (!node.getId().equals(ownID)) {
            // TODO Request get to the node
            return null;
        }

        String filePath = dbFolder + key;
        byte[] value;

        try (FileInputStream fis = new FileInputStream(filePath)) {
            value = fis.readAllBytes();
        } catch (IOException e) {
            System.out.println("Error opening file in get operation: " + filePath);
            e.printStackTrace();
            return null;
        }

        return value;
    }

    @Override
    public void delete(String key) {
        Node node = getResponsibleNode(key);
        if (!node.getId().equals(ownID)) {
            // TODO Request delete to the node
            return;
        }

        String filePath = dbFolder + key;
        File file = new File(filePath);
        if (!file.delete()) {
            System.out.println("Error deleting the file: " + filePath);
        }
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

        try {
            File file = new File(folderPath + "test");
            file.createNewFile();
        } catch (Exception err) {
            System.out.println("ERROR");
        }
        return folderPath;
    }
}
