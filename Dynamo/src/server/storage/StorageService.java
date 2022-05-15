package server.storage;

import server.cluster.Node;

import java.util.Map;
import java.util.TreeMap;

public class StorageService implements KeyValue {
    private final TreeMap<String, Node> nodeMap;
    private final String ownID;

    public StorageService(TreeMap<String, Node> nodeMap, String ownID) {
        this.nodeMap = nodeMap;
        this.ownID = ownID;
    }

    @Override
    public void put(String key, byte[] value) {
        Node node = getResponsibleNode(key);
        if (!node.getId().equals(ownID)) {
            // TODO Request put to the node
            return;
        }

        // TODO Put operation
    }

    @Override
    public byte[] get(String key) {
        Node node = getResponsibleNode(key);
        if (!node.getId().equals(ownID)) {
            // TODO Request get to the node
            return new byte[0];
        }

        // TODO Get operation
        return new byte[0];
    }

    @Override
    public void delete(String key) {
        Node node = getResponsibleNode(key);
        if (!node.getId().equals(ownID)) {
            // TODO Request delete to the node
            return;
        }

        // TODO Delete operation
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
}
