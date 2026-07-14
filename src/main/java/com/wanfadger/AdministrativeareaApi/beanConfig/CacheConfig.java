package com.wanfadger.AdministrativeareaApi.beanConfig;

import com.wanfadger.AdministrativeareaApi.cache.AreaCacheProperties;
import com.wanfadger.AdministrativeareaApi.cache.CacheValueSerializer;
import com.wanfadger.AdministrativeareaApi.cache.TwoLevelCacheManager;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.cache.TwoLevelCache;
import io.lettuce.core.ClientOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.BatchStrategies;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The single home for cache wiring.
 *
 * <p>The cache is two-tier: Caffeine in the heap (L1) in front of Redis over the network (L2). The
 * interesting logic lives in {@code com.wanfadger.AdministrativeareaApi.cache}; this class assembles
 * it.
 */
@Configuration
@EnableConfigurationProperties(AreaCacheProperties.class)
@Slf4j
public class CacheConfig implements CachingConfigurer {

    /**
     * Stable cache key from the request query-map: drops blank values (so a client sending
     * {@code partOf=} doesn't fork the key), sorts entries so filter order can't change the key,
     * and braces them — {@code {page:1, size:50, type:COUNTY}} — so keys stay readable in RedisInsight.
     *
     * <p>The map reaching this generator has already been through {@code AreaQueryFactory}, so it
     * contains only whitelisted, clamped, normalised keys. That is load-bearing: this generator
     * hashes whatever it is given, so before canonicalisation existed, {@code ?type=REGION&_cb=<random>}
     * minted an unbounded number of distinct cache entries, each holding a full page. Any client
     * could have exhausted both tiers' memory with a loop.
     *
     * <p>Lives on the outer class, not inside the {@code @Profile("!test")} block below: it has no
     * Redis dependency, and profile-scoping it is the only reason the test config once had to carry a
     * copy-pasted duplicate. Two definitions of a cache-key generator can drift, and any drift is a
     * cache-correctness bug that tests are structurally incapable of catching — they would be
     * exercising a different key generator than production.
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
     * The cache the application actually uses. {@code @Primary} because the {@link RedisCacheManager}
     * below is also a {@link CacheManager} bean — it is L2, an implementation detail of this one, and
     * nothing should inject it by mistake.
     */
    @Bean
    @Primary
    public TwoLevelCacheManager cacheManager(ObjectProvider<RedisCacheManager> l2,
                                             AreaCacheProperties props) {
        RedisCacheManager redis = props.isL2Enabled() ? l2.getIfAvailable() : null;
        return new TwoLevelCacheManager(redis, props);
    }

    /**
     * Keeps the application up when Redis is not.
     *
     * <p><b>This is a second net, not the primary one.</b> Spring does not consult a
     * {@code CacheErrorHandler} on the {@code @Cacheable(sync = true)} path at all — which is the path
     * every read in this application takes. Resilience to a Redis outage is implemented inside
     * {@code TwoLevelCache}, which guards each L2 call individually and degrades to a miss. This
     * handler covers the remaining paths (and anything added later that forgets to).
     *
     * <p>Swallowing a failed {@code clear} is the one uncomfortable case: it means a write's
     * invalidation did not reach L2 and some pods may serve stale reads until the TTL expires. The
     * alternative is failing the write — rejecting a legitimate database change because a cache is
     * unavailable. Stale reads of reference data that changes monthly are the better failure.
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache GET failed [{}::{}] — reading through to the database", cache.getName(), key, e);
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("Cache PUT failed [{}::{}] — value not cached", cache.getName(), key, e);
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache EVICT failed [{}::{}] — entry may be stale until it expires", cache.getName(), key, e);
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("Cache CLEAR failed [{}] — region may be stale until it expires", cache.getName(), e);
            }
        };
    }

    // ----------------------------------------------------------------------------------- L2 (Redis)
    //
    // @Profile("!test") sits on the bean methods rather than on a nested @Configuration class: it is
    // only these two beans that need Redis, and the test profile excludes RedisAutoConfiguration
    // entirely, so a RedisConnectionFactory parameter would have nothing to bind to. Without the
    // profile guard the test context would fail to start; with it, TwoLevelCacheManager simply gets a
    // null L2 and runs L1-only, which is a topology it supports.

    /**
     * The one Lettuce setting that has no property equivalent.
     *
     * <p>Spring Boot already auto-configures Lettuce, and the timeouts belong in
     * {@code application-dev1.properties} ({@code spring.data.redis.timeout} /
     * {@code .connect-timeout}) — not here. What cannot be expressed as a property is Lettuce's
     * behaviour while the connection is <i>down</i>: by default it <b>accepts and queues</b> commands,
     * betting the connection returns, and each queued command then sits until it ages out against the
     * command timeout.
     *
     * <p>That default is how the resilience story quietly became false. Measured against a stopped
     * Redis, one {@code /search} took <b>120 seconds</b> and then returned 200 — the stock 60s command
     * timeout, paid twice, because the read path touches L2 twice (a {@code get}, then a {@code put}
     * of the value it just loaded from the database). It "succeeded", which is exactly what made it
     * dangerous: under traffic, every request holds its state for two minutes and the queue of doomed
     * commands grows without bound, while the service reports itself healthy.
     *
     * <p>{@code REJECT_COMMANDS} fails them immediately instead, so {@link TwoLevelCache} catches the
     * exception and reads through to Postgres. A Redis outage then costs approximately nothing per
     * request rather than a timeout. Auto-reconnect is Lettuce's default and is left alone, so
     * recovery needs no intervention.
     */
    @Bean
    @Profile("!test")
    public LettuceClientConfigurationBuilderCustomizer lettuceFailFast() {
        return builder -> builder.clientOptions(ClientOptions.builder()
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .build());
    }

    /**
     * Values carry {@code @class} type hints so the DTO subtypes round-trip back to their concrete
     * type, and a whitelist restricts which classes those hints may name — see
     * {@link CacheValueSerializer}, where both halves are explained.
     */
    private RedisCacheConfiguration redisCacheConfig(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(CacheValueSerializer.create()));
    }

    @Bean
    @Profile("!test")
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory,
                                               AreaCacheProperties props) {
        // The default cache writer implements clear() with KEYS — a single blocking O(N) command that
        // stalls the whole Redis instance, for every other tenant of it too, while it runs. Since a
        // write to REGION clears six regions, that ran up to six times per write. BatchStrategies.scan()
        // uses a cursor instead: incremental, non-blocking, 500 keys at a time.
        RedisCacheWriter writer = RedisCacheWriter
                .nonLockingRedisCacheWriter(connectionFactory, BatchStrategies.scan(500));

        // The TTL split that CacheValueKeyConfig has documented since day one and nothing ever
        // implemented: both regions were built from a single 30-minute default.
        Map<String, RedisCacheConfiguration> perRegion = new HashMap<>();
        for (AdministrativeAreaType type : AdministrativeAreaType.values()) {
            perRegion.put(CacheValueKeyConfig.item(type), redisCacheConfig(props.getItemTtl()));
            perRegion.put(CacheValueKeyConfig.search(type), redisCacheConfig(props.getSearchTtl()));
        }

        return RedisCacheManager.builder(writer)
                .cacheDefaults(redisCacheConfig(props.getSearchTtl()))
                .withInitialCacheConfigurations(perRegion)
                .initialCacheNames(CacheValueKeyConfig.allCacheNames())
                .build();
    }
}
