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
     * Redis caching beans: a single primary {@link RedisCacheManager} (30-minute TTL) backing
     * both read regions — the paginated search and the full-detail getOne — plus a
     * {@code searchKeyGenerator} that produces stable keys from the request query-map.
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

        /**
         * Single cache manager backing both read regions — the paginated {@code search} and the
         * full-detail {@code getOne}. A 30-minute TTL keeps cached pages fresh; every write
         * ({@code create}/{@code update}/{@code delete}) evicts the regions immediately.
         */
        @Bean
        @Primary
        public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(cacheConfig(Duration.ofMinutes(30)))
                    .build();
        }

        /**
         * Stable cache key for {@code Map<String,String> queryMap} search params: drops blank
         * values (so {@code partOf=} the client sends empty doesn't fork the key or hurt the hit
         * rate), sorts the entries so identical filters always resolve to the same key regardless
         * of order, and wraps them in braces — {@code {size:1000, type:COUNTY}} — so the param
         * object stays readable in a Redis browser even when many filters are present.
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
                            .filter(e -> e.getValue() != null && !String.valueOf(e.getValue()).isBlank())
                            .sorted(Map.Entry.comparingByKey((a, b) ->
                                    String.valueOf(a).compareTo(String.valueOf(b))))
                            .map(e -> e.getKey() + ":" + e.getValue())
                            .collect(Collectors.joining(", ", "{", "}"));
                }
                return "SimpleKey " + Arrays.deepToString(params);
            };
        }
    }
}
