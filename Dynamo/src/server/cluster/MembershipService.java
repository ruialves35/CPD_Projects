package server.cluster;

import server.storage.StorageService;

import java.util.ArrayList;
import java.util.List;

public class MembershipService implements ClusterMembership {
    private final List<Node> nodes;
    private final StorageService storageService;

    public MembershipService(final StorageService storageService) {
        nodes = new ArrayList<>();
        this.storageService = storageService;
    }

    @Override
    public void join() {
        // TODO Join protocol
        Node newNode = new Node("temp");
        nodes.add(newNode);
        storageService.addNode(newNode);
    }

    @Override
    public void leave() {
        // TODO Leave protocol
        if (nodes.size() > 0) nodes.remove(0);
    }
}
