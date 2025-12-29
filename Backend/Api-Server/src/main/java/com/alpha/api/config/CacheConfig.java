package com.alpha.api.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Multi-layer Cache Configuration
 * - L1 Cache: Caffeine (In-memory, fast)
 * - L2 Cache: Redis (Distributed, persistent)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${app.cache.l1.ttl:10s}")
    private String l1Ttl;

    @Value("${app.cache.l1.max-size:10000}")
    private long l1MaxSize;

    /**
     * L1 Cache: Caffeine (In-memory)
     * - TTL: 10 seconds
     * - Max Size: 10,000 entries
     */
    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(parseDuration(l1Ttl).getSeconds(), TimeUnit.SECONDS)
                .maximumSize(l1MaxSize)
                .recordStats());
        return cacheManager;
    }

    /**
     * Reactive Redis Template for manual cache operations
     * - Used for L2 cache in reactive environment
     * - Uses RedisSerializer.json() for Spring Boot 4.0 compatibility
     */
    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {

        // Use RedisSerializer.json() instead of deprecated Jackson2JsonRedisSerializer
        RedisSerializationContext<String, Object> serializationContext = RedisSerializationContext
                .<String, Object>newSerializationContext(new StringRedisSerializer())
                .key(new StringRedisSerializer())
                .value(RedisSerializer.json())
                .hashKey(new StringRedisSerializer())
                .hashValue(RedisSerializer.json())
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }

    /**
     * Parse duration string (e.g., "10s", "10m")
     */
    private Duration parseDuration(String duration) {
        if (duration.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(duration.substring(0, duration.length() - 1)));
        } else if (duration.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(duration.substring(0, duration.length() - 1)));
        } else if (duration.endsWith("h")) {
            return Duration.ofHours(Long.parseLong(duration.substring(0, duration.length() - 1)));
        }
        return Duration.ofSeconds(10); // default
    }
}
