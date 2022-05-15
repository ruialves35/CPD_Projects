package server.storage;

import server.cluster.Node;

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

    private Node getResponsibleNode(String key) {
        return null;
    }
}
