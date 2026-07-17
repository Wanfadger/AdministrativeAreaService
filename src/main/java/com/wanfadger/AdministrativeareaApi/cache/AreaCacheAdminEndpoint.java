package com.wanfadger.AdministrativeareaApi.cache;

import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Operator control over the cache, exposed as an actuator endpoint at {@code /actuator/areacache}.
 *
 * <h2>Why an actuator endpoint and not a REST controller</h2>
 *
 * Wiping the cache is an <b>operational</b> action, not part of the public API, and it is a genuine
 * weapon: an unauthenticated "drop all caches" on the main port is a denial-of-service handle — one
 * request sends every subsequent read to Postgres until the cache refills. Actuator lives on the
 * separate management port (5401), which is deliberately <b>not published to the host</b> in
 * docker-compose; Prometheus and the healthcheck reach it inside the network and nothing outside can.
 * Putting the eviction here inherits that isolation instead of re-opening it on the public port.
 *
 * <p>In development, where you run the API from the IDE, the management port is on localhost, so:
 * <pre>
 *   curl -X DELETE http://localhost:5401/actuator/areacache            # everything
 *   curl -X DELETE http://localhost:5401/actuator/areacache?type=PARISH # one level and below
 *   curl        http://localhost:5401/actuator/areacache               # what is cached right now
 * </pre>
 *
 * <h2>Why not the built-in {@code /actuator/caches}</h2>
 *
 * That endpoint exists and even calls {@code clear()} on each region — but {@code clear()} alone is
 * only half a correct eviction here. It does not bump the ETag versions (so a client holding an old
 * {@code If-None-Match} would get a {@code 304} against fresh data — a stale response nothing can
 * expire) and it does not broadcast (so every <i>other</i> pod keeps serving its stale L1 for up to
 * the L1 TTL). {@link AreaCacheInvalidator#invalidateAll()} does all three; this endpoint is a thin
 * shell over it.
 */
@Component
@Endpoint(id = "areacache")
@RequiredArgsConstructor
public class AreaCacheAdminEndpoint {

    private final AreaCacheInvalidator invalidator;
    private final TwoLevelCacheManager cacheManager;

    /**
     * A snapshot of what each region is holding in the in-heap tier. L2 (Redis) size is deliberately
     * not queried — that would mean a {@code SCAN} per region on every poll of this endpoint.
     */
    @ReadOperation
    public Map<String, Object> state() {
        Map<String, Object> regions = new TreeMap<>();
        long total = 0;
        for (TwoLevelCache cache : cacheManager.allCaches()) {
            long size = cache.getNativeCache().estimatedSize();
            regions.put(cache.getName(), size);
            total += size;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("l1EntriesTotal", total);
        body.put("regions", regions);
        return body;
    }

    /**
     * Evict the cache. With no {@code type}, evicts every region cluster-wide; with a {@code type}
     * (either the enum name {@code PARISH} or the display form {@code "SUB COUNTY"}), evicts that level
     * and every level below it — the same cascade a write to that level triggers.
     *
     * @param type optional level to scope the eviction to
     * @return what was evicted, or a 400-shaped body if {@code type} was given but unrecognised
     */
    @DeleteOperation
    public Map<String, Object> evict(@Nullable String type) {
        if (type == null || type.isBlank()) {
            invalidator.invalidateAll();
            return Map.of("evicted", "all", "regions", cacheManager.getCacheNames().size());
        }

        Optional<AdministrativeAreaType> parsed = AdministrativeAreaType.fromStr(type);
        if (parsed.isEmpty()) {
            return Map.of(
                    "evicted", "none",
                    "error", "unknown type '" + type + "'",
                    "validTypes", java.util.Arrays.stream(AdministrativeAreaType.values()).map(Enum::name).toList());
        }

        AdministrativeAreaType t = parsed.get();
        // No transaction is active on an actuator call, so invalidate() runs the eviction immediately
        // rather than deferring it to an afterCommit hook.
        invalidator.invalidate(t);
        return Map.of("evicted", t.name(), "cascade", t.selfAndDescendants().stream().map(Enum::name).toList());
    }
}
