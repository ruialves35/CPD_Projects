package server.cluster;

public interface ClusterMembership {
    boolean join();
    void leave();
}
