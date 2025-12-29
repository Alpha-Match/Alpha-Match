package com.alpha.api.application.service;

import com.alpha.api.domain.cache.port.CachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Cache Service (Application Layer)
 * - Business logic for multi-layer caching
 * - Cache-aside pattern implementation
 * - Uses CachePort (Domain) instead of Infrastructure dependencies
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final CachePort l1Cache; // Caffeine
    private final CachePort l2Cache; // Redis

    // Default TTLs
    private static final Duration L1_TTL = Duration.ofSeconds(10);
    private static final Duration L2_TTL = Duration.ofMinutes(10);

    /**
     * Get from cache with fallback to source
     * - L1 (Caffeine) → L2 (Redis) → Source
     * - Populates upper layers on cache miss
     *
     * @param key Cache key
     * @param valueType Value class type
     * @param source Data source (DB query)
     * @param <T> Value type
     * @return Mono of value
     */
    public <T> Mono<T> getOrLoad(String key, Class<T> valueType, Supplier<Mono<T>> source) {
        log.debug("Cache lookup: key={}", key);

        return l1Cache.get(key, valueType)
                .doOnNext(value -> log.debug("L1 cache HIT: key={}", key))
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("L1 cache MISS: key={}", key);
                    return l2Cache.get(key, valueType)
                            .doOnNext(value -> log.debug("L2 cache HIT: key={}", key))
                            .flatMap(value -> {
                                // Populate L1 on L2 hit
                                return l1Cache.put(key, value, L1_TTL)
                                        .thenReturn(value);
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                log.debug("L2 cache MISS: key={}, loading from source", key);
                                return source.get()
                                        .flatMap(value -> {
                                            // Populate both L1 and L2 on source load
                                            return Mono.when(
                                                    l1Cache.put(key, value, L1_TTL),
                                                    l2Cache.put(key, value, L2_TTL)
                                            ).thenReturn(value);
                                        });
                            }));
                }));
    }

    /**
     * Invalidate cache entry (both L1 and L2)
     *
     * @param key Cache key
     * @return Mono of Boolean (true if successful)
     */
    public Mono<Boolean> invalidate(String key) {
        log.info("Cache invalidate: key={}", key);
        return Mono.when(
                l1Cache.invalidate(key),
                l2Cache.invalidate(key)
        ).thenReturn(true);
    }

    /**
     * Invalidate all caches with given prefix
     *
     * @param keyPrefix Key prefix pattern (e.g., "recruit:*")
     * @return Mono of Long (number of keys deleted)
     */
    public Mono<Long> invalidateByPrefix(String keyPrefix) {
        log.info("Cache invalidate by prefix: keyPrefix={}", keyPrefix);
        return Mono.zip(
                l1Cache.invalidateByPrefix(keyPrefix),
                l2Cache.invalidateByPrefix(keyPrefix)
        ).map(tuple -> tuple.getT1() + tuple.getT2());
    }

    /**
     * Invalidate all caches
     *
     * @return Mono of Long (number of keys deleted)
     */
    public Mono<Long> invalidateAll() {
        log.info("Cache invalidate all");
        return invalidateByPrefix("*");
    }

    /**
     * Cache key builder for recruit
     *
     * @param recruitId Recruit ID
     * @return Cache key
     */
    public static String recruitKey(String recruitId) {
        return "recruit:" + recruitId;
    }

    /**
     * Cache key builder for candidate
     *
     * @param candidateId Candidate ID
     * @return Cache key
     */
    public static String candidateKey(String candidateId) {
        return "candidate:" + candidateId;
    }

    /**
     * Cache key builder for skill categories
     *
     * @return Cache key
     */
    public static String skillCategoriesKey() {
        return "skill:categories";
    }

    /**
     * Cache key builder for dashboard data
     *
     * @param userMode User mode (CANDIDATE or RECRUITER)
     * @return Cache key
     */
    public static String dashboardKey(String userMode) {
        return "dashboard:" + userMode;
    }
}
