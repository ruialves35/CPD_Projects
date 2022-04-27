package server;

public interface KeyValue {
    void put(String key, byte[] value);
    byte[] get(String key);
    void delete(String key);
}
