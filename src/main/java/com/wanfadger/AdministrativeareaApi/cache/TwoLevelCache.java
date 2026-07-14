package com.wanfadger.AdministrativeareaApi.cache;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.lang.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A cache region backed by Caffeine in the heap (L1) and Redis over the network (L2).
 *
 * <pre>
 *   read  ─▶ L1  (~100ns, per-pod)
 *             │ miss
 *             ▼
 *           L2  (~1-3ms, shared)  ──hit──▶ populate L1
 *             │ miss
 *             ▼
 *           loader (Postgres)     ────────▶ populate L1 + L2
 * </pre>
 *
 * <p>Extends {@link AbstractValueAdaptingCache} rather than implementing {@link Cache} directly, so
 * that "cached, and the value is null" and "not cached" stay distinguishable — the base class carries
 * that through {@code toStoreValue}/{@code fromStoreValue} and the {@code NullValue} sentinel. Hand-
 * rolling it is how caches end up re-querying the database every time for a key that legitimately has
 * no value.
 *
 * <h2>Why {@link #get(Object, Callable)} is implemented here rather than inherited</h2>
 *
 * This is the method behind {@code @Cacheable(sync = true)}, and it is the reason this class exists
 * at all.
 *
 * <p>Spring Data's own {@code RedisCache} implements it with a <b>cache-wide {@code synchronized}
 * block held across the Redis round-trip and the database load</b>. This application runs on Java 21
 * with {@code spring.threads.virtual.enabled=true}. A virtual thread that blocks inside
 * {@code synchronized} <b>pins its carrier thread</b> — the carrier cannot be reused while the
 * virtual thread waits on I/O. So under load, every request that misses this cache would park a real
 * OS thread for the duration of a database query, and they would all queue behind one monitor.
 * Switching on {@code sync = true} over a stock {@code RedisCacheManager} would therefore have made
 * throughput <i>worse</i> than no single-flight at all, while looking like an optimisation.
 *
 * <p>{@link ReentrantLock} unmounts a virtual thread cleanly instead of pinning, and the locks here
 * are <b>per key</b> (striped), not per cache — so a slow parish query no longer blocks a region
 * lookup. The single-flight it buys is worth having on its own: when a hot page expires, a hundred
 * concurrent requests for it issue <b>one</b> query, not a hundred.
 *
 * <h2>Why L2 failures are swallowed here rather than by the CacheErrorHandler</h2>
 *
 * {@code CacheErrorHandler} is <b>not consulted on the {@code sync = true} path</b> — Spring's
 * {@code CacheAspectSupport.handleSynchronizedGet()} has no error wrapping around it. So if Redis is
 * down, a {@code RedisConnectionFailureException} thrown from L2 would propagate straight out of the
 * cache and become a 500, and no {@code CacheErrorHandler} anywhere would see it. Every L2 call below
 * is therefore individually guarded: a failing L2 degrades to a miss and the read is served from the
 * database. The {@code CacheErrorHandler} registered in {@code CacheConfig} is a second net for the
 * non-sync paths, not the primary one.
 */
@Slf4j
public class TwoLevelCache extends AbstractValueAdaptingCache {

    private final String name;
    private final com.github.benmanes.caffeine.cache.Cache<Object, Object> l1;

    /** Null when running without Redis (single-instance deployments, and the test profile). */
    @Nullable
    private final Cache l2;

    private final ReentrantLock[] loadLocks;

    @Getter private final LongAdder l1Hits = new LongAdder();
    @Getter private final LongAdder l2Hits = new LongAdder();
    @Getter private final LongAdder misses = new LongAdder();
    /** Redis calls that threw. A non-zero rate here means the app is serving from L1 + DB only. */
    @Getter private final LongAdder l2Errors = new LongAdder();

    public TwoLevelCache(String name,
                         com.github.benmanes.caffeine.cache.Cache<Object, Object> l1,
                         @Nullable Cache l2,
                         int lockStripes) {
        super(true);   // allow null values — see the class javadoc
        this.name = name;
        this.l1 = l1;
        this.l2 = l2;

        int stripes = Integer.highestOneBit(Math.max(1, lockStripes));   // round down to a power of 2
        this.loadLocks = new ReentrantLock[stripes];
        for (int i = 0; i < stripes; i++) {
            this.loadLocks[i] = new ReentrantLock();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    /** The Caffeine tier. Micrometer binds to this; the invalidation listener clears it. */
    @Override
    public com.github.benmanes.caffeine.cache.Cache<Object, Object> getNativeCache() {
        return l1;
    }

    // ------------------------------------------------------------------ reads

    /**
     * The single read path. Returns the <i>store</i> value ({@code NullValue.INSTANCE} for a cached
     * null, {@code null} for absent); {@link AbstractValueAdaptingCache} unwraps it for callers.
     */
    @Override
    protected Object lookup(Object key) {
        Object cached = l1.getIfPresent(key);
        if (cached != null) {
            l1Hits.increment();
            return cached;
        }

        ValueWrapper fromL2 = l2Get(key);
        if (fromL2 != null) {
            // L2 hands back the user value; L1 stores the adapted store value.
            Object storeValue = toStoreValue(fromL2.get());
            l1.put(key, storeValue);
            l2Hits.increment();
            return storeValue;
        }

        misses.increment();
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Callable<T> valueLoader) {
        Object cached = lookup(key);
        if (cached != null) {
            return (T) fromStoreValue(cached);
        }

        ReentrantLock lock = lockFor(key);
        lock.lock();
        try {
            // Someone else may have loaded this key while we waited for the lock. Peek at L1 only:
            // whoever loaded it populated L1 on the way out, and re-running the full lookup() would
            // pay for a second Redis round-trip and skew the hit/miss counters.
            Object loadedByAnother = l1.getIfPresent(key);
            if (loadedByAnother != null) {
                l1Hits.increment();
                return (T) fromStoreValue(loadedByAnother);
            }

            T value;
            try {
                value = valueLoader.call();
            } catch (Throwable t) {
                // Contract: the loader's exception must reach the caller unchanged after unwrapping,
                // so a NotFoundException from the service still becomes a 404 and is NOT cached.
                throw new ValueRetrievalException(key, valueLoader, t);
            }
            put(key, value);
            return value;
        } finally {
            lock.unlock();
        }
    }

    // ------------------------------------------------------------------ writes

    @Override
    public void put(Object key, @Nullable Object value) {
        l1.put(key, toStoreValue(value));
        if (l2 != null) {
            try {
                l2.put(key, value);   // RedisCache adapts the value itself
            } catch (RuntimeException e) {
                l2Failed("put", key, e);
            }
        }
    }

    /**
     * Not atomic across the two tiers. Nothing in this application calls it — {@code @Cacheable} uses
     * {@code get}/{@code put} — and making it atomic across a network hop would require a distributed
     * lock, which would cost more than the method is worth.
     */
    @Override
    @Nullable
    public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
        ValueWrapper existing = get(key);
        if (existing != null) {
            return existing;
        }
        put(key, value);
        return null;
    }

    @Override
    public void evict(Object key) {
        l1.invalidate(key);
        if (l2 != null) {
            try {
                l2.evict(key);
            } catch (RuntimeException e) {
                l2Failed("evict", key, e);
            }
        }
    }

    @Override
    public void clear() {
        l1.invalidateAll();
        if (l2 != null) {
            try {
                l2.clear();
            } catch (RuntimeException e) {
                l2Failed("clear", "*", e);
            }
        }
    }

    /**
     * Clear the in-heap tier only, leaving Redis alone.
     *
     * <p>This is what a pod does when it receives a cross-pod invalidation message: the pod that
     * performed the write already evicted the shared L2, so every other pod has exactly one stale
     * copy to discard — its own. Calling {@link #clear()} here instead would have every pod in the
     * cluster redundantly issue a Redis {@code SCAN}+{@code DEL} for keys that are already gone.
     */
    public void clearLocal() {
        l1.invalidateAll();
    }

    // ------------------------------------------------------------------ internals

    @Nullable
    private ValueWrapper l2Get(Object key) {
        if (l2 == null) {
            return null;
        }
        try {
            return l2.get(key);
        } catch (RuntimeException e) {
            l2Failed("get", key, e);
            return null;   // degrade to a miss — the loader will hit the database
        }
    }

    private void l2Failed(String op, Object key, RuntimeException e) {
        l2Errors.increment();
        // Not logged at ERROR: this path is survivable by design, and a Redis outage under load would
        // otherwise produce a log line per request and turn a degradation into an outage of its own.
        log.warn("L2 cache {} failed for region '{}' key '{}' — serving without Redis ({}: {})",
                op, name, key, e.getClass().getSimpleName(), e.getMessage());
    }

    private ReentrantLock lockFor(Object key) {
        int h = key.hashCode();
        h ^= (h >>> 16);   // spread — cache keys are structured strings with clustered low bits
        return loadLocks[h & (loadLocks.length - 1)];
    }
}
