package server.storage;

import server.cluster.Node;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class StorageService implements KeyValue {
    private final Map<String, Node> nodeMap;

    public StorageService(final Node firstNode) {
        nodeMap = new HashMap<>();
        addNode(firstNode);
    }

    public void addNode(final Node node) {
        nodeMap.put(generateKey(node.getId()), node);
    }

    public static String generateKey(final String hashable) {
        return generateKey(hashable.getBytes(StandardCharsets.UTF_8));
    }

    public static String generateKey(final byte[] hashable) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Arrays.toString(digest.digest(hashable));
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Invalid MessageDigest algorithm");
            System.exit(1);
        }
        return null;
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
