package com.cse444.threadtrace.common.storage;

import com.cse444.threadtrace.common.config.TraceabilityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Storage adapter targeting MinIO S3 API via HTTP REST.
 * Gracefully falls back to local storage if MinIO is not reachable or in restricted sandbox environments.
 */
@Component("minioStorageAdapter")
public class MinioStorageAdapter implements StoragePort {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageAdapter.class);

    private final TraceabilityProperties properties;
    private final LocalDiskStorageAdapter fallbackLocal;
    private volatile HttpClient httpClient;

    public MinioStorageAdapter(TraceabilityProperties properties, LocalDiskStorageAdapter fallbackLocal) {
        this.properties = properties;
        this.fallbackLocal = fallbackLocal;
    }

    private HttpClient getHttpClient() {
        if (httpClient == null) {
            synchronized (this) {
                if (httpClient == null) {
                    try {
                        httpClient = HttpClient.newBuilder()
                                .connectTimeout(Duration.ofSeconds(3))
                                .build();
                    } catch (Throwable t) {
                        log.warn("HttpClient initialization not available in current environment: {}", t.getMessage());
                    }
                }
            }
        }
        return httpClient;
    }

    @Override
    public String store(String key, byte[] data, String contentType) {
        HttpClient client = getHttpClient();
        if (client != null && properties.storage() != null && properties.storage().endpoint() != null) {
            try {
                String endpoint = properties.storage().endpoint();
                String bucket = properties.storage().bucket();
                String url = endpoint + "/" + bucket + "/" + key;
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", contentType != null ? contentType : "application/octet-stream")
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(data))
                        .timeout(Duration.ofSeconds(5))
                        .build();

                HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return key;
                }
            } catch (Exception e) {
                log.warn("MinIO upload failed ({}), falling back to local disk storage", e.getMessage());
            }
        }
        return fallbackLocal.store(key, data, contentType);
    }

    @Override
    public byte[] retrieve(String key) {
        HttpClient client = getHttpClient();
        if (client != null && properties.storage() != null && properties.storage().endpoint() != null) {
            try {
                String endpoint = properties.storage().endpoint();
                String bucket = properties.storage().bucket();
                String url = endpoint + "/" + bucket + "/" + key;
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .timeout(Duration.ofSeconds(5))
                        .build();
                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() == 200) {
                    return response.body();
                }
            } catch (Exception e) {
                log.warn("MinIO retrieve failed ({}), falling back to local disk storage", e.getMessage());
            }
        }
        return fallbackLocal.retrieve(key);
    }

    @Override
    public boolean exists(String key) {
        return fallbackLocal.exists(key);
    }

    @Override
    public void delete(String key) {
        fallbackLocal.delete(key);
    }

    @Override
    public String computeSha256(byte[] data) {
        return fallbackLocal.computeSha256(data);
    }
}
