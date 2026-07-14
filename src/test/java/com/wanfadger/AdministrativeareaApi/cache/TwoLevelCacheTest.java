package com.wanfadger.AdministrativeareaApi.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The three properties of {@link TwoLevelCache} that are easy to get wrong and impossible to notice:
 * a cached null re-querying the database forever, a stampede that looks like it works, and a Redis
 * outage that takes the service down with it.
 */
class TwoLevelCacheTest {

    private com.github.benmanes.caffeine.cache.Cache<Object, Object> l1() {
        return Caffeine.newBuilder().maximumSize(100).recordStats().build();
    }

    private TwoLevelCache cache(Cache l2) {
        return new TwoLevelCache("aa_item_REGION", l1(), l2, 16);
    }

    // ---------------------------------------------------------------- null semantics

    /**
     * "Cached, and the value is null" must be distinguishable from "not cached". Conflating them means
     * a key with no value is re-queried on every single request, forever — a cache that reports a
     * healthy hit rate while doing no work at all for the one key that needs it most.
     */
    @Test
    void aCachedNullIsAHit_notAMiss() {
        TwoLevelCache cache = cache(new ConcurrentMapCache("l2"));

        assertThat(cache.get("absent")).as("never written").isNull();

        cache.put("known-null", null);

        Cache.ValueWrapper wrapper = cache.get("known-null");
        assertThat(wrapper).as("written, so it is a HIT").isNotNull();
        assertThat(wrapper.get()).as("and the hit's value is null").isNull();
    }

    /** The loader must run once for a cached null, not on every call. */
    @Test
    void aCachedNull_doesNotReInvokeTheLoader() {
        TwoLevelCache cache = cache(new ConcurrentMapCache("l2"));
        AtomicInteger loads = new AtomicInteger();

        for (int i = 0; i < 5; i++) {
            String value = cache.get("k", () -> {
                loads.incrementAndGet();
                return null;
            });
            assertThat(value).isNull();
        }
        assertThat(loads).hasValue(1);
    }

    // ---------------------------------------------------------------- tier promotion

    @Test
    void anL1Miss_isServedFromL2_andPopulatesL1() {
        Cache l2 = new ConcurrentMapCache("l2");
        TwoLevelCache cache = cache(l2);

        l2.put("k", "from-l2");   // seed L2 only — as if another pod had cached it

        assertThat(cache.getL1Hits().sum()).isZero();

        assertThat(cache.get("k").get()).isEqualTo("from-l2");
        assertThat(cache.getL2Hits().sum()).as("first read came from L2").isEqualTo(1);

        assertThat(cache.get("k").get()).isEqualTo("from-l2");
        assertThat(cache.getL1Hits().sum()).as("L2 hit promoted the value into L1").isEqualTo(1);
        assertThat(cache.getL2Hits().sum()).as("and L2 was not consulted again").isEqualTo(1);
    }

    // ---------------------------------------------------------------- single-flight

    /**
     * The reason {@code sync = true} exists. Without per-key locking, a hot key expiring under load
     * means every concurrent request for it independently queries the database.
     */
    @Test
    void concurrentRequestsForOneKey_runTheLoaderExactlyOnce() throws Exception {
        TwoLevelCache cache = cache(new ConcurrentMapCache("l2"));
        AtomicInteger loads = new AtomicInteger();

        int threads = 32;
        CountDownLatch startTogether = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    try {
                        startTogether.await();
                        cache.get("hot", () -> {
                            loads.incrementAndGet();
                            Thread.sleep(50);   // a database query
                            return "loaded";
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            startTogether.countDown();
            assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(loads).as("32 concurrent requests, ONE query").hasValue(1);
        assertThat(cache.get("hot").get()).isEqualTo("loaded");
    }

    /** Distinct keys must not serialise behind one another — the locks are per key, not per cache. */
    @Test
    void distinctKeys_loadConcurrently() throws Exception {
        // One stripe per key here would be luck; 512 stripes makes a collision between 4 keys unlikely.
        TwoLevelCache cache = new TwoLevelCache("aa_item_REGION", l1(), new ConcurrentMapCache("l2"), 512);
        CountDownLatch allInsideTheLoader = new CountDownLatch(4);

        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 4; i++) {
                String key = "key-" + i;
                pool.submit(() -> cache.get(key, () -> {
                    allInsideTheLoader.countDown();
                    // Deadlocks if the loads are serialised by a shared lock.
                    allInsideTheLoader.await(5, TimeUnit.SECONDS);
                    return key;
                }));
            }
            assertThat(allInsideTheLoader.await(5, TimeUnit.SECONDS))
                    .as("all four loaders ran at once")
                    .isTrue();
        }
    }

    // ---------------------------------------------------------------- L2 resilience

    /**
     * Redis being down must degrade the cache, not the service.
     *
     * <p>This cannot be delegated to a {@code CacheErrorHandler}: Spring does not consult one on the
     * {@code sync = true} path, which is the path every read in this application takes. So the
     * resilience has to be inside the cache, and this test is what proves it is.
     */
    @Test
    void whenL2Throws_readsStillSucceedFromTheLoader() {
        TwoLevelCache cache = cache(new ExplodingCache());
        AtomicInteger loads = new AtomicInteger();

        String value = cache.get("k", () -> {
            loads.incrementAndGet();
            return "from-db";
        });

        assertThat(value).isEqualTo("from-db");
        assertThat(loads).hasValue(1);
        assertThat(cache.getL2Errors().sum()).as("the failure was counted, not swallowed silently").isPositive();

        // ...and L1 still works, so the outage costs one Redis round-trip, not the request.
        assertThat(cache.get("k").get()).isEqualTo("from-db");
        assertThat(cache.getL1Hits().sum()).isPositive();
    }

    @Test
    void whenL2Throws_writesAndEvictionsDoNotPropagate() {
        TwoLevelCache cache = cache(new ExplodingCache());

        assertThatCode(() -> {
            cache.put("k", "v");
            cache.evict("k");
            cache.clear();
        }).doesNotThrowAnyException();

        assertThat(cache.getL2Errors().sum()).isEqualTo(3);
    }

    /** A loader that throws must NOT be cached, and the cause must reach the caller intact. */
    @Test
    void aFailingLoader_propagatesAndCachesNothing() {
        TwoLevelCache cache = cache(new ConcurrentMapCache("l2"));

        assertThatThrownBy(() -> cache.get("k", () -> {
            throw new IllegalStateException("area not found");
        }))
                .isInstanceOf(Cache.ValueRetrievalException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("area not found");

        assertThat(cache.get("k")).as("a failed load must not be cached").isNull();
    }

    /** Every operation fails, the way a Redis outage fails. */
    private static class ExplodingCache implements Cache {
        private RuntimeException boom() {
            return new RedisConnectionFailureException("connection refused");
        }

        @Override public String getName() { return "exploding"; }
        @Override public Object getNativeCache() { return this; }
        @Override public ValueWrapper get(Object key) { throw boom(); }
        @Override public <T> T get(Object key, Class<T> type) { throw boom(); }
        @Override public <T> T get(Object key, java.util.concurrent.Callable<T> loader) { throw boom(); }
        @Override public void put(Object key, Object value) { throw boom(); }
        @Override public void evict(Object key) { throw boom(); }
        @Override public void clear() { throw boom(); }
    }
}
