package server.storage;

import server.cluster.Node;

import java.util.SortedMap;

public class TransferService {
    private final SortedMap<String, Node> nodeMap;

    public TransferService(SortedMap<String, Node> nodeMap) {
        this.nodeMap = nodeMap;
    }

    public void join(Node node) {
        // TODO
    }

    public void leave(Node node) {
        // TODO
    }
}
