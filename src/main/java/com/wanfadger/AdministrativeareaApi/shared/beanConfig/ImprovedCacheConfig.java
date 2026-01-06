package com.wanfadger.AdministrativeareaApi.shared.beanConfig;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Improved Cache Configuration with optimizations for high-traffic scenarios
 * 
 * Key improvements:
 * - Shorter, meaningful cache prefixes
 * - TTL on all cache managers (including primary)
 * - Custom key generator for deterministic cache keys
 * - Optimized Redis connection handling
 * 
 * Migration from CacheConfig:
 * 1. Backup existing CacheConfig.java
 * 2. Replace with this implementation
 * 3. Update controller to use queryMapKeyGenerator
 */
@Configuration
public class ImprovedCacheConfig {

    @Value("${spring.data.redis.host}")
    String host;

    @Value("${spring.data.redis.port}")
    int port;

    /**
     * Jedis connection factory
     * 
     * Note: Connection pooling is configured via application properties:
     * spring.data.redis.jedis.pool.max-active=200
     * spring.data.redis.jedis.pool.max-idle=50
     * spring.data.redis.jedis.pool.min-idle=10
     */
    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        JedisConnectionFactory factory = new JedisConnectionFactory(config);
        
        // Connection pooling is handled by Jedis internally
        // Configure via application.properties for explicit control
        
        return factory;
    }

    @Bean
    public RedisTemplate<?, ?> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

        RedisTemplate<?, ?> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setDefaultSerializer(serializer);
        redisTemplate.setEnableTransactionSupport(false);

        return redisTemplate;
    }

    /**
     * Custom cache key generator for query parameter maps
     * Ensures deterministic cache keys regardless of Map iteration order
     */
    @Bean("queryMapKeyGenerator")
    public KeyGenerator queryMapKeyGenerator() {
        return new KeyGenerator() {
            @Override
            public Object generate(Object target, Method method, Object... params) {
                if (params.length > 0 && params[0] instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> queryMap = (Map<String, String>) params[0];
                    if (queryMap == null || queryMap.isEmpty()) {
                        return method.getName() + ":default";
                    }
                    return queryMap.entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .map(e -> e.getKey() + "=" + (e.getValue() != null ? e.getValue() : ""))
                            .collect(Collectors.joining("&"));
                }
                return method.getName() + ":" + (params.length > 0 ? params[0] : "default");
            }
        };
    }

    /**
     * Primary cache manager with 30-minute TTL
     * Used for frequently changing data
     */
    @Bean
    @Primary
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("adminArea.")  // Shorter, meaningful prefix
                .entryTtl(Duration.ofMinutes(30))   // Added TTL (was missing)
                .disableCachingNullValues()
                .serializeValuesWith(
                    org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer())
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    /**
     * Cache manager with 1-hour TTL
     * For moderately stable data
     */
    @Bean("hourCacheManager")
    public RedisCacheManager hourCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("adminArea.hour.")
                .entryTtl(Duration.ofHours(1))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    /**
     * Cache manager with 24-hour TTL
     */
    @Bean("_24HourCacheManager")
    public RedisCacheManager _24HourCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("adminArea.24h.")
                .entryTtl(Duration.ofHours(24))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    /**
     * Cache manager with 7-day TTL
     * For stable, rarely-changing data
     */
    @Bean("weekCacheManager")
    public RedisCacheManager weekCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("adminArea.week.")
                .entryTtl(Duration.ofDays(7))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    /**
     * Cache manager with 30-day TTL
     * For very stable data
     */
    @Bean("monthCacheManager")
    public RedisCacheManager monthCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("adminArea.month.")
                .entryTtl(Duration.ofDays(30))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
