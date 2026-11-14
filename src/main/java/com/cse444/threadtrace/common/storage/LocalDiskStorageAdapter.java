package com.cse444.threadtrace.common.storage;

import com.cse444.threadtrace.common.config.TraceabilityProperties;
import com.cse444.threadtrace.common.error.DomainException;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
@Primary
public class LocalDiskStorageAdapter implements StoragePort {

    private final Path storageDirectory;

    public LocalDiskStorageAdapter(TraceabilityProperties properties) {
        String pathStr = properties.storage() != null
                ? properties.storage().resolveLocalStoragePath()
                : "./storage";
        this.storageDirectory = Paths.get(pathStr).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageDirectory);
        } catch (IOException e) {
            throw new DomainException("STORAGE_INIT_ERROR", "Failed to initialize local storage directory: " + e.getMessage());
        }
    }

    @Override
    public String store(String key, byte[] data, String contentType) {
        try {
            Path targetPath = storageDirectory.resolve(key).normalize();
            if (!targetPath.startsWith(storageDirectory)) {
                throw new DomainException("INVALID_STORAGE_PATH", "Path traversal detected: " + key);
            }
            if (targetPath.getParent() != null) {
                Files.createDirectories(targetPath.getParent());
            }
            Files.write(targetPath, data);
            return key;
        } catch (IOException e) {
            throw new DomainException("STORAGE_ERROR", "Failed to store file: " + e.getMessage());
        }
    }

    @Override
    public byte[] retrieve(String key) {
        try {
            Path targetPath = storageDirectory.resolve(key).normalize();
            if (!targetPath.startsWith(storageDirectory) || !Files.exists(targetPath)) {
                throw new DomainException("FILE_NOT_FOUND", "Evidence file not found: " + key);
            }
            return Files.readAllBytes(targetPath);
        } catch (IOException e) {
            throw new DomainException("STORAGE_ERROR", "Failed to read file: " + e.getMessage());
        }
    }

    @Override
    public boolean exists(String key) {
        Path targetPath = storageDirectory.resolve(key).normalize();
        return Files.exists(targetPath) && targetPath.startsWith(storageDirectory);
    }

    @Override
    public void delete(String key) {
        try {
            Path targetPath = storageDirectory.resolve(key).normalize();
            if (targetPath.startsWith(storageDirectory)) {
                Files.deleteIfExists(targetPath);
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    public String computeSha256(byte[] data) {
        if (data == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
