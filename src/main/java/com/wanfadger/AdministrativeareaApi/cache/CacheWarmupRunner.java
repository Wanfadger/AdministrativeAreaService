package com.wanfadger.AdministrativeareaApi.cache;

import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.service.AdministrativeAreaService;
import com.wanfadger.AdministrativeareaApi.service.query.AreaQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fills the cache at startup, so a pod is warm before it is allowed to take traffic.
 *
 * <p>{@link ApplicationRunner} runs before the readiness probe flips, which is the whole point: a
 * pod that joins the load balancer cold serves its first few hundred requests from the database,
 * and during a rolling deploy that is every pod, one after another, at exactly the moment the old
 * warm pods are being killed.
 *
 * <h2>The one way this silently fails</h2>
 *
 * By warming keys nobody reads.
 *
 * <p>A cache key here is derived from the query map, so a warmup that builds its own map — even one
 * that <i>looks</i> right — produces a different key from the one the controller will look up, and
 * the warmed entries sit there until they expire while every real request misses. The hit rate stays
 * at zero and the startup log cheerfully reports "warmed 39 entries". It is a bug with no symptom
 * except the absence of the benefit.
 *
 * <p>The defence is structural: this class does not construct cache keys at all. It builds the same
 * <b>raw request params</b> a client sends, hands them to the same {@link AreaQueryFactory#canonicalise}
 * the controller calls, and invokes the same {@code @Cacheable} service the controller invokes. There
 * is no second code path that could drift from the first.
 *
 * <p>That is also why the sizes below are configurable and are documented as having to match the
 * client. {@code size=25} and {@code size=2000} are not arbitrary: they are what the explorer table
 * and the map actually request. A warmup at {@code size=50} would be a different key, and therefore
 * useless — correct-looking, and worth nothing.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheWarmupRunner implements ApplicationRunner {

    /** The {@code @Primary} caching facade — the same bean the controller holds. */
    private final AdministrativeAreaService areas;
    private final AreaQueryFactory queryFactory;
    private final AreaCacheProperties props;

    /** One request to replay, described exactly as a client would send it. */
    private record WarmupRequest(AdministrativeAreaType type, String label, Map<String, String> params) {
    }

    @Override
    public void run(ApplicationArguments args) {
        AreaCacheProperties.Warmup config = props.getWarmup();
        if (!config.isEnabled()) {
            log.info("Cache warmup disabled (app.cache.warmup.enabled=false)");
            return;
        }

        List<WarmupRequest> requests = plan(config);
        long startedAt = System.nanoTime();
        int warmed = 0;
        int failed = 0;

        for (WarmupRequest request : requests) {
            try {
                // canonicalise() then search() — byte-for-byte the controller's own path, which is
                // what guarantees these keys are the keys that will later be read.
                areas.search(queryFactory.canonicalise(request.type(), request.params()));
                warmed++;
            } catch (RuntimeException e) {
                // Never fail startup over a cache. A cold pod is slow; a pod that will not start is
                // an outage.
                failed++;
                log.warn("Cache warmup failed for {} [{}] — continuing cold for that entry ({}: {})",
                        request.type(), request.label(), e.getClass().getSimpleName(), e.getMessage());
            }
        }

        Duration took = Duration.ofNanos(System.nanoTime() - startedAt);
        if (failed > 0) {
            log.warn("Cache warmup: {} entries warmed, {} FAILED, in {}ms", warmed, failed, took.toMillis());
        } else {
            log.info("Cache warmup: {} entries warmed in {}ms", warmed, took.toMillis());
        }
    }

    /**
     * What to warm.
     *
     * <p>Each entry mirrors a request the clients genuinely make on first load. Anything not listed
     * here is not warmed — notably the tree's per-parent child lookups and any {@code partOf} or
     * free-text filter, which are unbounded and could not be enumerated even in principle.
     */
    private List<WarmupRequest> plan(AreaCacheProperties.Warmup config) {
        List<WarmupRequest> requests = new ArrayList<>();

        for (AdministrativeAreaType type : AdministrativeAreaType.values()) {
            // The API's own default page — no params at all. This is what an integrating service
            // gets when it calls /search?type=X and nothing else, so it is the single most likely
            // request in the whole system.
            requests.add(new WarmupRequest(type, "api default page", Map.of()));

            // The console dashboard's six count tiles: one row fetched, only totalElements read.
            requests.add(new WarmupRequest(type, "dashboard count",
                    params("page", "1", "size", "1", "view", "flat")));

            // The map, per level. The heaviest entries by far, and the ones a cold pod most needs.
            if (config.isIncludeMapPulls()) {
                requests.add(new WarmupRequest(type, "map pull",
                        params("size", String.valueOf(config.getMapSize()), "sortBy", "name", "view", "flat")));
            }
        }

        // The explorer's landing state: the region tree root, and the first page of the region table.
        requests.add(new WarmupRequest(AdministrativeAreaType.REGION, "tree root",
                params("size", String.valueOf(config.getTreeSize()), "sortBy", "name", "view", "flat")));
        requests.add(new WarmupRequest(AdministrativeAreaType.REGION, "level table page 1",
                params("page", "1", "size", String.valueOf(config.getTableSize()),
                        "sortBy", "name", "sortDirection", "asc", "view", "flat")));

        return requests;
    }

    private static Map<String, String> params(String... keyValues) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }
}
