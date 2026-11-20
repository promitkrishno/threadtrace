package com.cse444.threadtrace.passport.web;

import com.cse444.threadtrace.passport.projection.PublicPassportView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory cache for published Digital Product Passports (FR7, QA4).
 * Guarantees sub-2s HTTP 200 responses for public consumers even when PostgreSQL is offline.
 */
@Service
public class PassportCacheService {

    private static final Logger log = LoggerFactory.getLogger(PassportCacheService.class);

    private final ConcurrentMap<String, CachedPassportEntry> cache = new ConcurrentHashMap<>();

    public record CachedPassportEntry(
            PublicPassportView view,
            String etag,
            byte[] qrBytes,
            Instant cachedAt
    ) {}

    public void put(String qrToken, Integer version, PublicPassportView view, String etag, byte[] qrBytes) {
        String key = buildKey(qrToken, version);
        cache.put(key, new CachedPassportEntry(view, etag, qrBytes, Instant.now()));
        // Also cache as latest if version is specified or default
        cache.put(buildKey(qrToken, null), new CachedPassportEntry(view, etag, qrBytes, Instant.now()));
        log.debug("Cached public passport with key: {}", key);
    }

    public Optional<CachedPassportEntry> get(String qrToken, Integer version) {
        String key = buildKey(qrToken, version);
        return Optional.ofNullable(cache.get(key));
    }

    public boolean contains(String qrToken, Integer version) {
        return cache.containsKey(buildKey(qrToken, version));
    }

    public void clear() {
        cache.clear();
    }

    public int size() {
        return cache.size();
    }

    private String buildKey(String qrToken, Integer version) {
        return (qrToken != null ? qrToken : "") + ":" + (version != null ? version : "latest");
    }
}
