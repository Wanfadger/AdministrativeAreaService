package com.wanfadger.AdministrativeareaApi;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Test-profile cache wiring: provides in-memory {@link ConcurrentMapCacheManager}s under the
 * same bean names the service's {@code @Cacheable}/{@code @CacheEvict} annotations reference
 * (hourCacheManager, weekCacheManager, searchCacheManager, …) plus the {@code searchKeyGenerator},
 * so caching works in tests without Redis. The real {@code CacheConfig} is {@code @Profile("!test")}.
 */
@TestConfiguration
public class TestCacheConfig {

    /**
     * One shared in-memory manager behind every named bean. This mirrors Redis, where all
     * the production cache managers share a single keyspace keyed by cache name — so a
     * {@code @CacheEvict} resolved against the primary manager still clears what a
     * {@code @Cacheable(cacheManager="weekCacheManager")} wrote. Separate manager instances
     * would not share entries and eviction across managers would silently miss.
     */
    private final ConcurrentMapCacheManager shared = new ConcurrentMapCacheManager();

    @Bean
    @Primary
    public CacheManager cacheManager() {
        return shared;
    }

    @Bean("hourCacheManager")
    public CacheManager hourCacheManager() {
        return shared;
    }

    @Bean("_24HourCacheManager")
    public CacheManager _24HourCacheManager() {
        return shared;
    }

    @Bean("weekCacheManager")
    public CacheManager weekCacheManager() {
        return shared;
    }

    @Bean("monthCacheManager")
    public CacheManager monthCacheManager() {
        return shared;
    }

    @Bean("searchCacheManager")
    public CacheManager searchCacheManager() {
        return shared;
    }

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
