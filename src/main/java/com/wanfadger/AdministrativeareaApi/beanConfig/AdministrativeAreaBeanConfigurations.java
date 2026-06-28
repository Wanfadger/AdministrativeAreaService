package com.wanfadger.AdministrativeareaApi.beanConfig;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
public class AdministrativeAreaBeanConfigurations {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * Redis caching beans, mirroring the URRMS Core multi-tier model: a primary default
     * manager plus explicit-TTL managers selectable per cache region, a short-lived
     * {@code searchCacheManager} for paginated search results, and a {@code searchKeyGenerator}
     * that produces stable keys from the request query-map.
     *
     * <p>The connection factory is Spring Boot's auto-configured {@code LettuceConnectionFactory}
     * (Spring's default Redis client), driven by {@code spring.data.redis.*} properties incl.
     * {@code spring.data.redis.lettuce.pool.*} for pooling (requires commons-pool2).
     *
     * <p>Values are serialized with {@link GenericJackson2JsonRedisSerializer} (writes
     * {@code @class} type hints) so Spring {@code @Cacheable} reads round-trip back to the exact
     * DTO type. NOTE: this is a JVM-typed on-wire format — flush Redis on deploy if migrating
     * from the previous plain-JSON cache, and do not read these keys from a non-JVM client.
     *
     * <p>Disabled under the {@code test} profile (tests provide in-memory cache managers).
     */
    @Configuration
    @Profile("!test")
    static class RedisCacheBeans {

        private GenericJackson2JsonRedisSerializer jsonSerializer() {
            return new GenericJackson2JsonRedisSerializer();
        }

        private RedisCacheConfiguration cacheConfig(Duration ttl) {
            return RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(ttl)
                    .disableCachingNullValues()
                    .serializeKeysWith(RedisSerializationContext.SerializationPair
                            .fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair
                            .fromSerializer(jsonSerializer()));
        }

        @Bean
        public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
            RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
            redisTemplate.setConnectionFactory(redisConnectionFactory);
            redisTemplate.setKeySerializer(new StringRedisSerializer());
            redisTemplate.setValueSerializer(jsonSerializer());
            redisTemplate.setHashKeySerializer(new StringRedisSerializer());
            redisTemplate.setHashValueSerializer(jsonSerializer());
            redisTemplate.afterPropertiesSet();
            return redisTemplate;
        }

        /** Primary manager — 1 hour default TTL for per-key entity reads. */
        @Bean
        @Primary
        public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(cacheConfig(Duration.ofHours(1)))
                    .build();
        }

        @Bean("hourCacheManager")
        public RedisCacheManager hourCacheManager(RedisConnectionFactory connectionFactory) {
            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(cacheConfig(Duration.ofHours(1)))
                    .build();
        }

        @Bean("_24HourCacheManager")
        public RedisCacheManager _24HourCacheManager(RedisConnectionFactory connectionFactory) {
            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(cacheConfig(Duration.ofHours(24)))
                    .build();
        }

        @Bean("weekCacheManager")
        public RedisCacheManager weekCacheManager(RedisConnectionFactory connectionFactory) {
            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(cacheConfig(Duration.ofDays(7)))
                    .build();
        }

        @Bean("monthCacheManager")
        public RedisCacheManager monthCacheManager(RedisConnectionFactory connectionFactory) {
            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(cacheConfig(Duration.ofDays(30)))
                    .build();
        }

        /** Short-TTL manager for paginated search results — pages churn as data changes. */
        @Bean("searchCacheManager")
        public RedisCacheManager searchCacheManager(RedisConnectionFactory connectionFactory) {
            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(cacheConfig(Duration.ofMinutes(30)))
                    .build();
        }

        /**
         * Stable cache key for {@code Map<String,String> queryMap} search params: sorts the
         * entries so identical filters always resolve to the same key regardless of order.
         */
        @Bean
        public KeyGenerator searchKeyGenerator() {
            return (target, method, params) -> {
                if (params.length == 0) {
                    return "SimpleKey []";
                }
                Object param = params[0];
                if (param instanceof Map<?, ?> map) {
                    return map.entrySet().stream()
                            .sorted(Map.Entry.comparingByKey((a, b) ->
                                    String.valueOf(a).compareTo(String.valueOf(b))))
                            .map(e -> e.getKey() + ":" + e.getValue())
                            .collect(Collectors.joining(","));
                }
                return "SimpleKey " + Arrays.deepToString(params);
            };
        }
    }
}
