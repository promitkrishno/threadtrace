package com.cse444.threadtrace.common.storage;

public interface StoragePort {
    String store(String key, byte[] data, String contentType);
    byte[] retrieve(String key);
    boolean exists(String key);
    void delete(String key);
    String computeSha256(byte[] data);
}
