package com.wanfadger.AdministrativeareaApi.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.wanfadger.AdministrativeareaApi.beanConfig.CacheValueKeyConfig;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NullValue;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the twelve {@link TwoLevelCache} regions and hands them out.
 *
 * <p>All twelve are created eagerly in the constructor and the map is then immutable. Two reasons:
 * a region that does not exist until someone queries its level cannot be bound to Micrometer at
 * startup, so its hit rate would simply be missing from the dashboard rather than zero — and an
 * immutable map needs no locking on the read path, which is every request.
 */
@Slf4j
public class TwoLevelCacheManager implements CacheManager {

    private final Map<String, TwoLevelCache> caches;

    /**
     * @param l2Manager the Redis cache manager, or {@code null} to run L1-only. Null is a supported
     *                  topology, not a degraded one: a single-instance deployment has nobody to share
     *                  L2 with, and the test profile uses it so that tests exercise this exact class
     *                  rather than a stand-in that could behave differently.
     */
    public TwoLevelCacheManager(@Nullable CacheManager l2Manager, AreaCacheProperties props) {
        Map<String, TwoLevelCache> built = new LinkedHashMap<>();

        for (String region : CacheValueKeyConfig.allCacheNames()) {
            Cache l2 = l2Manager == null ? null : l2Manager.getCache(region);
            built.put(region, new TwoLevelCache(region, buildL1(region, props), l2, props.getLockStripes()));
        }

        this.caches = Map.copyOf(built);
        log.info("Two-level cache ready: {} regions, L1 ttl={}, L2={}",
                caches.size(), props.getL1Ttl(), l2Manager == null ? "disabled (L1 only)" : "redis");
    }

    private com.github.benmanes.caffeine.cache.Cache<Object, Object> buildL1(
            String region, AreaCacheProperties props) {

        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .expireAfterWrite(props.getL1Ttl())
                .recordStats();   // required for CaffeineCacheMetrics to report anything at all

        if (CacheValueKeyConfig.isSearchRegion(region)) {
            // A search entry holds a whole page of rows, so entries are not a meaningful unit —
            // see AreaCacheProperties#searchMaxWeight.
            int depth = CacheValueKeyConfig.typeOf(region).ordinal() + 1;
            return builder
                    .maximumWeight(props.getSearchMaxWeight())
                    .weigher((key, value) -> weigh(value, depth))
                    .build();
        }
        return builder.maximumSize(props.getItemMaxSize()).build();
    }

    /**
     * Weight of a cached value, in row-units.
     *
     * <p>Rows, scaled by how much ancestry each row drags with it: a cached page of 2,000 parishes is
     * 2,000 rows × 6 levels of embedded ancestry, and costs roughly six times what a page of 2,000
     * regions costs. Counting entries instead would treat those two as equal and let the parish
     * region quietly consume an order of magnitude more heap than its budget.
     */
    static int weigh(@Nullable Object storeValue, int depth) {
        int rows = 1;
        if (storeValue instanceof PaginatedResponseDTO<?> page) {
            Collection<?> data = page.getData();
            rows = (data == null || data.isEmpty()) ? 1 : data.size();
        } else if (storeValue instanceof NullValue) {
            rows = 1;
        }
        return rows * depth;
    }

    @Override
    @Nullable
    public Cache getCache(String name) {
        return caches.get(name);
    }

    @Override
    public Collection<String> getCacheNames() {
        return caches.keySet();
    }

    /** The concrete regions, for Micrometer binding and for the cross-pod invalidation listener. */
    public Collection<TwoLevelCache> allCaches() {
        return caches.values();
    }

    /** Clear the in-heap tier of the given regions, leaving Redis untouched. */
    public void clearLocal(Set<String> regions) {
        for (String region : regions) {
            TwoLevelCache cache = caches.get(region);
            if (cache != null) {
                cache.clearLocal();
            }
        }
    }

    /** Every region affected by a write at {@code type} — its own level and all levels below. */
    public List<TwoLevelCache> cascadeFrom(AdministrativeAreaType type) {
        return CacheValueKeyConfig.cascadeFrom(type).stream()
                .map(caches::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}
