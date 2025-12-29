package com.alpha.api.domain.cache.port;

import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Cache Port Interface (Domain Layer)
 * - Pure business interface for caching operations
 * - Technology-agnostic (no Caffeine/Redis dependencies)
 * - Implementation provided by Infrastructure Layer
 */
public interface CachePort {

    /**
     * Get value from cache
     *
     * @param key Cache key
     * @param valueType Value class type
     * @param <T> Value type
     * @return Mono of cached value (empty if not found)
     */
    <T> Mono<T> get(String key, Class<T> valueType);

    /**
     * Put value into cache
     *
     * @param key Cache key
     * @param value Value to cache
     * @param ttl Time to live
     * @return Mono of Boolean (true if successful)
     */
    Mono<Boolean> put(String key, Object value, Duration ttl);

    /**
     * Invalidate (delete) cache entry
     *
     * @param key Cache key
     * @return Mono of Boolean (true if successful)
     */
    Mono<Boolean> invalidate(String key);

    /**
     * Invalidate all caches with given prefix
     *
     * @param keyPrefix Key prefix pattern (e.g., "recruit:*")
     * @return Mono of Long (number of keys deleted)
     */
    Mono<Long> invalidateByPrefix(String keyPrefix);

    /**
     * Check if cache contains key
     *
     * @param key Cache key
     * @return Mono of Boolean (true if exists)
     */
    Mono<Boolean> exists(String key);
}
