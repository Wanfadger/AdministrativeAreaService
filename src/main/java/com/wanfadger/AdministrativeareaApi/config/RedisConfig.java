package com.wanfadger.AdministrativeareaApi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import redis.clients.jedis.JedisPoolConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    String host;

    @Value("${spring.data.redis.port}")
    int port;

    @Value("${spring.data.redis.password:}")
    String password;

    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        // Configure connection pool for production readiness
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(200); // Maximum connections
        poolConfig.setMaxIdle(50); // Maximum idle connections
        poolConfig.setMinIdle(10); // Minimum idle connections
        poolConfig.setMaxWaitMillis(5000); // Max wait time for connection
        poolConfig.setTestOnBorrow(true); // Test connection before use
        poolConfig.setTestOnReturn(true); // Test connection on return
        poolConfig.setTestWhileIdle(true); // Test idle connections
        poolConfig.setBlockWhenExhausted(true); // Block when pool exhausted

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        if (password != null && !password.isEmpty()) {
            config.setPassword(password);
        }

        JedisConnectionFactory factory = new JedisConnectionFactory(config);
        factory.setPoolConfig(poolConfig);
        factory.setTimeout(2000); // 2 seconds timeout
        return factory;
    }

    /**
     * Creates a JSON serializer without Java class metadata
     * This makes cached data language-agnostic and readable by any JSON parser
     * 
     * Uses plain JSON without @class fields, making it compatible with any language
     */
    private Jackson2JsonRedisSerializer<Object> createJsonSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        // Don't include type information - pure JSON without Java class metadata
        mapper.deactivateDefaultTyping();

        // Use constructor that takes ObjectMapper (non-deprecated approach)
        return new Jackson2JsonRedisSerializer<>(mapper, Object.class);
    }

    @Bean
    public RedisTemplate<?, ?> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        Jackson2JsonRedisSerializer<Object> serializer = createJsonSerializer();

        RedisTemplate<?, ?> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setDefaultSerializer(serializer);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(serializer);

        return redisTemplate;
    }

    @Bean
    @Primary
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        Jackson2JsonRedisSerializer<Object> serializer = createJsonSerializer();

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig() //
                .entryTtl(Duration.ofMinutes(30)) // Added TTL (was missing)
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return RedisCacheManager.builder(connectionFactory) //
                .cacheDefaults(config) //
                .build();
    }

    /**
     * Note: Additional cache managers (hourCacheManager, weekCacheManager, etc.)
     * were removed
     * because we now use service-level caching with explicit TTLs via
     * CacheHelperService.
     * 
     * The primary cacheManager is kept for:
     * - MonitoringConfig (health checks)
     * - Any future components that need Spring Cache annotations
     * 
     * Service-level caching uses RedisTemplate directly with keys like:
     * "AdministrativeAreas::code=123&type=REGION"
     * 
     * Cache eviction is handled via CacheHelperService.evictAll() in service
     * methods.
     */
}
