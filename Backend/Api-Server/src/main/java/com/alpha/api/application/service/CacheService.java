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

    // Static data TTLs (for Cache Warming)
    private static final Duration STATIC_L1_TTL = Duration.ofHours(24);
    private static final Duration STATIC_L2_TTL = Duration.ofHours(24);

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
     * Warm cache with static data (24-hour TTL)
     * - Used for skillCategories and dashboardData on application startup
     * - Populates both L1 (Caffeine) and L2 (Redis)
     * - Does NOT reload from source if already cached
     *
     * @param key Cache key
     * @param value Data to cache
     * @param <T> Value type
     * @return Mono of Boolean (true if successful)
     */
    public <T> Mono<Boolean> warmCache(String key, T value) {
        log.info("Cache warming: key={}", key);
        return Mono.when(
                l1Cache.put(key, value, STATIC_L1_TTL),
                l2Cache.put(key, value, STATIC_L2_TTL)
        ).thenReturn(true)
                .doOnSuccess(result -> log.info("Cache warmed successfully: key={}", key))
                .doOnError(error -> log.error("Cache warming failed: key={}, error={}", key, error.getMessage()));
    }

    /**
     * Get static data from cache (24-hour TTL)
     * - Used for skillCategories and dashboardData
     * - Falls back to source if not cached
     *
     * @param key Cache key
     * @param valueType Value class type
     * @param source Data source (DB query)
     * @param <T> Value type
     * @return Mono of value
     */
    public <T> Mono<T> getOrLoadStatic(String key, Class<T> valueType, Supplier<Mono<T>> source) {
        log.debug("Static cache lookup: key={}", key);

        return l1Cache.get(key, valueType)
                .doOnNext(value -> log.debug("L1 static cache HIT: key={}", key))
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("L1 static cache MISS: key={}", key);
                    return l2Cache.get(key, valueType)
                            .doOnNext(value -> log.debug("L2 static cache HIT: key={}", key))
                            .flatMap(value -> {
                                // Populate L1 on L2 hit
                                return l1Cache.put(key, value, STATIC_L1_TTL)
                                        .thenReturn(value);
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                log.debug("L2 static cache MISS: key={}, loading from source", key);
                                return source.get()
                                        .flatMap(value -> {
                                            // Populate both L1 and L2 on source load
                                            return Mono.when(
                                                    l1Cache.put(key, value, STATIC_L1_TTL),
                                                    l2Cache.put(key, value, STATIC_L2_TTL)
                                            ).thenReturn(value);
                                        });
                            }));
                }));
    }

    /**
     * Get static data from cache without type constraint (24-hour TTL)
     * - Used for complex types like List<T>
     * - Falls back to source if not cached
     * - Type safety is handled by caller
     *
     * @param key Cache key
     * @param source Data source (DB query)
     * @param <T> Value type
     * @return Mono of value
     */
    @SuppressWarnings("unchecked")
    public <T> Mono<T> getOrLoadStaticUnchecked(String key, Supplier<Mono<T>> source) {
        log.debug("Static cache lookup (unchecked): key={}", key);

        return l1Cache.get(key, Object.class)
                .map(obj -> (T) obj)
                .doOnNext(value -> log.debug("L1 static cache HIT: key={}", key))
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("L1 static cache MISS: key={}", key);
                    return l2Cache.get(key, Object.class)
                            .map(obj -> (T) obj)
                            .doOnNext(value -> log.debug("L2 static cache HIT: key={}", key))
                            .flatMap(value -> {
                                // Populate L1 on L2 hit
                                return l1Cache.put(key, value, STATIC_L1_TTL)
                                        .thenReturn(value);
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                log.debug("L2 static cache MISS: key={}, loading from source", key);
                                return source.get()
                                        .flatMap(value -> {
                                            // Populate both L1 and L2 on source load
                                            return Mono.when(
                                                    l1Cache.put(key, value, STATIC_L1_TTL),
                                                    l2Cache.put(key, value, STATIC_L2_TTL)
                                            ).thenReturn(value);
                                        });
                            }));
                }));
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

    /**
     * Cache key builder for search statistics
     * - Key includes mode, sorted skills, and limit for cache consistency
     * - Skills are joined with comma (already sorted by caller)
     *
     * @param mode User mode (CANDIDATE or RECRUITER)
     * @param sortedSkills Sorted list of skill names
     * @param limit Maximum number of top skills
     * @return Cache key
     */
    public static String searchStatisticsKey(String mode, java.util.List<String> sortedSkills, Integer limit) {
        String skillsHash = String.join(",", sortedSkills);
        return "searchStats:" + mode + ":" + skillsHash + ":" + limit;
    }
}
