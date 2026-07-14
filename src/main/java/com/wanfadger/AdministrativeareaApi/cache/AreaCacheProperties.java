package com.wanfadger.AdministrativeareaApi.cache;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Cache tunables ({@code app.cache.*}).
 *
 * <p>Named {@code AreaCacheProperties}, not {@code CacheProperties}, because Boot already ships an
 * {@code org.springframework.boot.autoconfigure.cache.CacheProperties} and having two types with the
 * same simple name in one codebase is a wrong-import waiting to happen.
 */
@ConfigurationProperties(prefix = "app.cache")
@Getter
@Setter
public class AreaCacheProperties {

    /**
     * L1 (in-heap) time-to-live.
     *
     * <p><b>Deliberately short, and this is the honest weak point of the design.</b> Cross-pod
     * invalidation goes over Redis pub/sub, which is fire-and-forget: a pod that is mid-GC-pause or
     * reconnecting to Redis when the message is published never sees it and keeps serving its stale
     * L1 copy. Nothing detects that. This TTL is the backstop — it bounds how long such a pod can be
     * wrong. Five minutes is the trade: long enough to absorb a traffic spike, short enough that a
     * missed invalidation is an inconvenience rather than an incident.
     *
     * <p>This is why it is not 30 minutes to match L2. L2 lives in Redis, is shared by every pod, and
     * is evicted synchronously by the writer — it has no such failure mode.
     */
    private Duration l1Ttl = Duration.ofMinutes(5);

    /** L2 TTL for single-item reads. */
    private Duration itemTtl = Duration.ofHours(1);

    /** L2 TTL for paginated search results. */
    private Duration searchTtl = Duration.ofMinutes(30);

    /**
     * Max entries in each of the six L1 <i>item</i> regions. One entry is one DTO, so this bound is
     * in the same unit as the thing being bounded and {@code maximumSize} is honest here.
     *
     * <p>5,000 is above the real row count at every level (the whole country is ~7,000 areas across
     * all six), so in practice these regions converge on holding everything — which is the point.
     */
    private long itemMaxSize = 5_000;

    /**
     * Max <b>weight</b> in each of the six L1 <i>search</i> regions, counted in rows, not entries.
     *
     * <p>{@code maximumSize} would be a trap here. A single {@code aa_search_PARISH} entry at
     * {@code size=2000} holds 2,000 parishes, each with six levels of ancestry hanging off it — call
     * it 12,000 objects and a couple of MB. Bounding that region at "500 entries" would authorise a
     * gigabyte. So entries are weighed by the number of rows they carry (scaled by the level's depth,
     * since a parish row drags five ancestors along and a region row drags none), and the bound is on
     * total weight.
     *
     * <p>20,000 row-units per region is roughly 40 MB at the parish level, worst case, and the levels
     * above are far cheaper. Comfortable inside the container's heap
     * ({@code -XX:MaxRAMPercentage=75}).
     */
    private long searchMaxWeight = 20_000;

    /**
     * Number of load locks per region. Must be a power of two.
     *
     * <p>These give cache-stampede protection: when a hot key expires, N concurrent requests for it
     * issue <b>one</b> query, not N. Striped rather than one-lock-per-key on purpose — a per-key map
     * has to evict its own entries, and every scheme for doing that either leaks locks or races with
     * a thread that is holding one. Striping is bounded, allocation-free and cannot race. The cost is
     * that two <i>different</i> keys landing on the same stripe serialise their loads; with 512
     * stripes that is rare, and the penalty is a parked virtual thread, not a blocked one.
     */
    private int lockStripes = 512;

    /**
     * Whether to use Redis as L2 at all. Off ⇒ Caffeine-only, which is a legitimate topology for a
     * single-instance deployment (and is what the test profile runs).
     */
    private boolean l2Enabled = true;

    /** Pre-loading the cache at startup, before the pod is allowed to take traffic. */
    private Warmup warmup = new Warmup();

    @Getter
    @Setter
    public static class Warmup {

        /**
         * Off by default in tests (see {@code application-test.properties}) — a warmup that queries
         * the database on every context start would slow the suite for no benefit.
         */
        private boolean enabled = true;

        /**
         * Rows the map view requests per level. Must match what the client actually sends, or the
         * warmed entry has a different cache key and is never read — see {@code CacheWarmupRunner}.
         */
        private int mapSize = 2000;

        /** Rows the explorer tree requests for its root level. */
        private int treeSize = 1000;

        /** Rows the explorer's level table requests for its first page. */
        private int tableSize = 25;

        /**
         * Skip the six 2,000-row map pulls. They are the bulk of the warmup's cost and the bulk of
         * its value; turn them off if startup time matters more than the first map render.
         */
        private boolean includeMapPulls = true;
    }
}
