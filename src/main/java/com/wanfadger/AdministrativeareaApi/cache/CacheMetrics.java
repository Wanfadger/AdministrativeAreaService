package com.wanfadger.AdministrativeareaApi.cache;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

/**
 * Publishes the cache's behaviour to Micrometer, per region.
 *
 * <p>Until now none of this existed. Spring's Redis caches are created lazily on first use, so at
 * startup there was nothing for Micrometer to bind to and the hit rate was simply <b>unobservable</b> —
 * which is the failure mode that lets a cache quietly do nothing for months while everyone assumes it
 * is working. All twelve regions are now created eagerly at startup precisely so that this class has
 * something to bind, and every one of them reports even if nobody has queried that level yet (a
 * missing series and a zero series look identical on a dashboard, and mean opposite things).
 *
 * <h2>What to actually watch</h2>
 *
 * <ul>
 *   <li>{@code aa.cache.l1.hits} vs {@code aa.cache.l2.hits} vs {@code aa.cache.misses}, by region —
 *       the tier breakdown. A high L2 hit rate with a low L1 hit rate means the in-heap tier is being
 *       evicted too aggressively (its weight bound is too tight, or writes are evicting too often).</li>
 *   <li>{@code aa.cache.l2.errors} — <b>the one to alert on.</b> Non-zero means Redis is failing and
 *       the service is quietly running on L1 + Postgres. It stays up, which is the point, but it is
 *       running without a shared cache and nothing else will tell you.</li>
 *   <li>{@code cache.evictions} / {@code cache.eviction.weight} (from Caffeine) — if these climb on
 *       the search regions, entries are being pushed out by the weight bound rather than expiring, and
 *       {@code app.cache.search-max-weight} is too low.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheMetrics {

    private final MeterRegistry registry;
    private final TwoLevelCacheManager cacheManager;

    @PostConstruct
    void bind() {
        for (TwoLevelCache cache : cacheManager.allCaches()) {
            // Caffeine's own stats: cache.gets, cache.puts, cache.evictions, cache.size, and — because
            // the L1 caches are built with recordStats() — hit/miss counts. Without recordStats() this
            // binder registers meters that are permanently zero, which is worse than no meters at all.
            CaffeineCacheMetrics.monitor(registry, cache.getNativeCache(), cache.getName());

            counter(cache, "aa.cache.l1.hits", "Reads served from the in-heap tier", TwoLevelCache::getL1Hits);
            counter(cache, "aa.cache.l2.hits", "Reads served from Redis (and promoted into L1)", TwoLevelCache::getL2Hits);
            counter(cache, "aa.cache.misses", "Reads that reached the database", TwoLevelCache::getMisses);
            counter(cache, "aa.cache.l2.errors", "Redis operations that failed; the read fell through to the database", TwoLevelCache::getL2Errors);
        }
        log.info("Bound cache metrics for {} regions", cacheManager.getCacheNames().size());
    }

    /**
     * A {@link FunctionCounter} rather than a {@code Counter}: the counts live in the cache's own
     * {@link LongAdder}s, which is where the hot read path can increment them without contention.
     * Micrometer reads them when it scrapes.
     */
    private void counter(TwoLevelCache cache, String name, String description,
                         Function<TwoLevelCache, LongAdder> value) {
        FunctionCounter.builder(name, cache, c -> value.apply(c).sum())
                .description(description)
                .tag("region", cache.getName())
                .register(registry);
    }
}
