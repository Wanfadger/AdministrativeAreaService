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
 * Test-profile cache wiring: provides a single in-memory {@link ConcurrentMapCacheManager} as the
 * primary {@link CacheManager} (matching production's single manager) plus the
 * {@code searchKeyGenerator}, so caching works in tests without Redis. The real cache beans are
 * {@code @Profile("!test")}.
 */
@TestConfiguration
public class TestCacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
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
