package server.storage;

import server.cluster.Node;

import java.util.TreeMap;

public class TransferService {
    private final TreeMap<String, Node> nodeMap;

    public TransferService(TreeMap<String, Node> nodeMap) {
        this.nodeMap = nodeMap;
    }

    public void join(Node node) {
        // TODO
    }

    public void leave(Node node) {
        // TODO
    }
}
