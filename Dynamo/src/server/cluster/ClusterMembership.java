package server.cluster;

public interface ClusterMembership {
    boolean join();
    boolean leave();
}
