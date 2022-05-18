package server.cluster;

public class Node {
    private final String id;
    private final int port;

    public Node(String id, int port) {
        this.id = id;
        this.port = port;
    }

    public String getId() {
        return id;
    }

    public int getPort() {
        return port;
    }
}
