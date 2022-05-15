package server.cluster;

public class Node {
    private final String id;

    public Node(final String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
