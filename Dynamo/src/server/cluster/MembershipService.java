package server.cluster;

import java.util.ArrayList;
import java.util.List;

public class MembershipService implements ClusterMembership {
    private List<Node> nodes;

    public MembershipService() {
        nodes = new ArrayList<>();
    }

    @Override
    public void join() {
        // TODO Join protocol
        nodes.add(new Node());
    }

    @Override
    public void leave() {
        // TODO Leave protocol
        if (nodes.size() > 0) nodes.remove(0);
    }
}
