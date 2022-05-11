package server.cluster;

import server.Utils;
import java.util.SortedMap;
import java.util.TreeMap;

public class MembershipService implements ClusterMembership {
    private final SortedMap<String, Node> nodeMap;

    public MembershipService() {
        nodeMap = new TreeMap<>();
    }

    @Override
    public void join() {
        // TODO Join protocol
        Node newNode = new Node("temp");
        String key = Utils.generateKey("temp");
        nodeMap.put(key, newNode);
    }

    @Override
    public void leave() {
        // TODO Leave protocol
        if (nodeMap.size() > 0) nodeMap.remove(Utils.generateKey("temp"));
    }

    public SortedMap<String, Node> getNodeMap() {
        return nodeMap;
    }
}
