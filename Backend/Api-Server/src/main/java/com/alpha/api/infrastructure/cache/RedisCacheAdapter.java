package com.alpha.api.infrastructure.cache;

import com.alpha.api.domain.cache.port.CachePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Redis Cache Adapter (Infrastructure Layer)
 * - L2 Cache implementation using Redis
 * - Distributed, persistent cache
 * - Reactive (Mono-based)
 */
@Slf4j
@Component("l2Cache")
@RequiredArgsConstructor
public class RedisCacheAdapter implements CachePort {

    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public <T> Mono<T> get(String key, Class<T> valueType) {
        return reactiveRedisTemplate.opsForValue()
                .get(key)
                .map(value -> objectMapper.convertValue(value, valueType))
                .doOnNext(value -> log.debug("Redis cache HIT: key={}", key))
                .onErrorResume(e -> {
                    log.warn("Redis cache GET error: key={}, error={}", key, e.getMessage());
                    return Mono.empty();
                });
    }

    @Override
    public Mono<Boolean> put(String key, Object value, Duration ttl) {
        return reactiveRedisTemplate.opsForValue()
                .set(key, value, ttl)
                .doOnNext(success -> log.debug("Redis cache PUT: key={}, ttl={}", key, ttl))
                .onErrorResume(e -> {
                    log.warn("Redis cache PUT error: key={}, error={}", key, e.getMessage());
                    return Mono.just(false);
                });
    }

    @Override
    public Mono<Boolean> invalidate(String key) {
        return reactiveRedisTemplate.delete(key)
                .map(count -> count > 0)
                .doOnNext(success -> log.debug("Redis cache DELETE: key={}, success={}", key, success))
                .onErrorResume(e -> {
                    log.warn("Redis cache DELETE error: key={}, error={}", key, e.getMessage());
                    return Mono.just(false);
                });
    }

    @Override
    public Mono<Long> invalidateByPrefix(String keyPrefix) {
        // Convert prefix pattern (e.g., "recruit:*") to Redis SCAN pattern
        return reactiveRedisTemplate.keys(keyPrefix)
                .flatMap(key -> reactiveRedisTemplate.delete(key))
                .reduce(0L, Long::sum)
                .doOnNext(count -> log.debug("Redis cache DELETE by prefix: keyPrefix={}, count={}", keyPrefix, count))
                .onErrorResume(e -> {
                    log.warn("Redis cache DELETE by prefix error: keyPrefix={}, error={}", keyPrefix, e.getMessage());
                    return Mono.just(0L);
                });
    }

    @Override
    public Mono<Boolean> exists(String key) {
        return reactiveRedisTemplate.hasKey(key)
                .onErrorResume(e -> {
                    log.warn("Redis cache EXISTS error: key={}, error={}", key, e.getMessage());
                    return Mono.just(false);
                });
    }
}
