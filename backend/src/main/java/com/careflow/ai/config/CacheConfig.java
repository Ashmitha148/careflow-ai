package com.careflow.ai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    public static final String DASHBOARD_SUMMARY_CACHE = "dashboardSummary";

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // RedisCacheManager builds lazily and does NOT connect during build(),
        // so a dead Redis would only surface later as a 500 on a cache write.
        // Test connectivity eagerly and fall back to an in-memory manager when
        // Redis is unavailable — the app stays up; caching just degrades.
        if (!isRedisAvailable(connectionFactory)) {
            log.warn("Redis not reachable — using in-memory ConcurrentMapCacheManager "
                    + "(dashboard caching degraded until Redis is available).");
            return new ConcurrentMapCacheManager(DASHBOARD_SUMMARY_CACHE);
        }

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(15))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(DASHBOARD_SUMMARY_CACHE, defaultConfig.entryTtl(Duration.ofSeconds(15)));

        log.info("Initialized RedisCacheManager with 15s TTL for dashboard summary aggregate cache.");
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    private boolean isRedisAvailable(RedisConnectionFactory connectionFactory) {
        try {
            return connectionFactory.getConnection().ping() != null;
        } catch (Exception e) {
            log.debug("Redis connectivity check failed: {}", e.getMessage());
            return false;
        }
    }
}
