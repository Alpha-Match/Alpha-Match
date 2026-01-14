package com.alpha.api.infrastructure.cache;

import com.alpha.api.domain.cache.port.CachePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Caffeine Cache Adapter (Infrastructure Layer)
 * - L1 Cache implementation using Caffeine
 * - In-memory, fast access
 * - Non-reactive wrapper (returns Mono for consistency)
 */
@Slf4j
@Component("l1Cache")
@RequiredArgsConstructor
public class CaffeineCacheAdapter implements CachePort {

    private final CacheManager caffeineCacheManager;
    private final ObjectMapper objectMapper;

    private static final String CACHE_NAME = "default";

    @Override
    public <T> Mono<T> get(String key, Class<T> valueType) {
        return Mono.fromSupplier(() -> {
            var cache = caffeineCacheManager.getCache(CACHE_NAME);
            if (cache == null) {
                return null;
            }
            var value = cache.get(key, valueType);
            if (value != null) {
                log.debug("Caffeine cache HIT: key={}", key);
            }
            return value;
        });
    }

    @Override
    public Mono<Boolean> put(String key, Object value, Duration ttl) {
        return Mono.fromRunnable(() -> {
            var cache = caffeineCacheManager.getCache(CACHE_NAME);
            if (cache != null) {
                cache.put(key, value);
                log.debug("Caffeine cache PUT: key={}", key);
            }
        }).thenReturn(true);
    }

    @Override
    public Mono<Boolean> invalidate(String key) {
        return Mono.fromRunnable(() -> {
            var cache = caffeineCacheManager.getCache(CACHE_NAME);
            if (cache != null) {
                cache.evict(key);
                log.debug("Caffeine cache EVICT: key={}", key);
            }
        }).thenReturn(true);
    }

    @Override
    public Mono<Long> invalidateByPrefix(String keyPrefix) {
        return Mono.fromSupplier(() -> {
            var cache = caffeineCacheManager.getCache(CACHE_NAME);
            if (cache != null) {
                cache.clear();
                log.debug("Caffeine cache CLEAR (all keys)");
                return 1L; // Cannot count individual keys in Caffeine
            }
            return 0L;
        });
    }

    @Override
    public Mono<Boolean> exists(String key) {
        return Mono.fromSupplier(() -> {
            var cache = caffeineCacheManager.getCache(CACHE_NAME);
            if (cache == null) {
                return false;
            }
            return cache.get(key) != null;
        });
    }
}
