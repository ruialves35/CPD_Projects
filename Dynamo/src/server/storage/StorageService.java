package server.storage;

import server.cluster.Node;
import java.util.SortedMap;

public class StorageService implements KeyValue {
    private final SortedMap<String, Node> nodeMap;

    public StorageService(final SortedMap<String, Node> nodeMap) {
        this.nodeMap = nodeMap;
    }

    @Override
    public void put(String key, byte[] value) {
        // TODO Put operation
    }

    @Override
    public byte[] get(String key) {
        // TODO Get operation
        return new byte[0];
    }

    @Override
    public void delete(String key) {
        // TODO Delete operation
    }
}
