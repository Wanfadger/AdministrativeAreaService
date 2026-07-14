package com.wanfadger.AdministrativeareaApi;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test-profile cache wiring: an in-memory {@link ConcurrentMapCacheManager} standing in for Redis,
 * whose beans are {@code @Profile("!test")}.
 *
 * <p>The {@code searchKeyGenerator} is deliberately NOT redefined here. It used to be copy-pasted
 * from the production config, so tests exercised a *different* key generator than production — and
 * any drift between the two would be a cache-correctness bug that tests could never catch. It now
 * lives on the un-profiled outer {@code CacheConfig} class and is shared by both.
 */
@TestConfiguration
public class TestCacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }
}
