package server.storage;

public interface KeyValue {
    byte[] put(String key, byte[] value);
    byte[] get(String key);
    byte[] delete(String key);
}
