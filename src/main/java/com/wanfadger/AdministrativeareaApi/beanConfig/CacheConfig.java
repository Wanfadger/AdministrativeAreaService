package com.wanfadger.AdministrativeareaApi.beanConfig;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The single home for cache wiring.
 *
 * <p>Replaces {@code AdministrativeAreaBeanConfigurations}, which also declared two beans that had
 * to go: a raw {@code new ObjectMapper()} (a top-level {@code ObjectMapper} bean backs off Boot's
 * {@code JacksonAutoConfiguration}, silently dropping {@code JavaTimeModule} — and the entities
 * carry {@code LocalDateTime}), and a {@code RedisTemplate} that nothing ever injected.
 *
 * <p>The {@code searchKeyGenerator} deliberately lives on the OUTER class, not inside the
 * {@code @Profile("!test")} block: it has no Redis dependency, and keeping it profile-scoped is the
 * only reason the test config had to carry a copy-pasted duplicate. Two definitions of a cache-key
 * generator can drift, and any drift is a cache-correctness bug that tests would not catch —
 * they'd be exercising a different key generator than production.
 */
@Configuration
@Slf4j
public class CacheConfig {

    /**
     * Stable cache key from the request query-map: drops blank values (so a client sending
     * {@code partOf=} doesn't fork the key), sorts entries so filter order can't change the key,
     * and braces them — {@code {size:1000, type:COUNTY}} — so keys stay readable in RedisInsight.
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

    /**
     * Redis cache manager. Disabled under {@code test}, where an in-memory manager stands in.
     *
     * <p>Values are serialized with {@link GenericJackson2JsonRedisSerializer}, which writes
     * {@code @class} type hints so the DTO subtypes round-trip back to their concrete type. That is
     * a JVM-typed on-wire format: flush Redis on deploy if the DTO packages move, and do not read
     * these keys from a non-JVM client.
     */
    @Configuration
    @Profile("!test")
    static class RedisCacheBeans {

        private RedisCacheConfiguration cacheConfig(Duration ttl) {
            return RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(ttl)
                    .disableCachingNullValues()
                    .serializeKeysWith(RedisSerializationContext.SerializationPair
                            .fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair
                            .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        }

        @Bean
        @Primary
        public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(cacheConfig(Duration.ofMinutes(30)))
                    .build();
        }
    }
}
