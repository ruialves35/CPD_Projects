package server.storage;

public interface KeyValue {
    String put(byte[] value);
    byte[] get(String key);
    void delete(String key);
}
