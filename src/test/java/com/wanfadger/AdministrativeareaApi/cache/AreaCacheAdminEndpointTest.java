package com.wanfadger.AdministrativeareaApi.cache;

import com.wanfadger.AdministrativeareaApi.beanConfig.CacheValueKeyConfig;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * The operator eviction endpoint. The behaviours that matter are the three the built-in
 * {@code /actuator/caches} does NOT provide — so they are what this asserts, on real cache and version
 * objects rather than mocks of them.
 */
class AreaCacheAdminEndpointTest {

    private TwoLevelCacheManager caches;
    private AreaVersionRegistry versions;
    private AreaCacheAdminEndpoint endpoint;

    @BeforeEach
    void setUp() {
        AreaCacheProperties props = new AreaCacheProperties();
        // Single-instance semantics: the local version counter is authoritative, so etagFor() returns a
        // real value we can watch move. With L2 "enabled" but no Redis wired, versions would be withheld
        // (null) and the ETag assertion could not be made.
        props.setL2Enabled(false);

        caches = new TwoLevelCacheManager(null, props);

        @SuppressWarnings("unchecked")
        ObjectProvider<StringRedisTemplate> noRedis = mock(ObjectProvider.class);
        versions = new AreaVersionRegistry(noRedis, props);

        AreaCacheInvalidator invalidator =
                new AreaCacheInvalidator(caches, CacheInvalidationBroadcaster.NOOP, versions);
        endpoint = new AreaCacheAdminEndpoint(invalidator, caches);
    }

    private void warmEveryRegion() {
        caches.allCaches().forEach(c -> c.put("k", "cached-value"));
    }

    private long l1Size(String region) {
        TwoLevelCache cache = (TwoLevelCache) caches.getCache(region);
        cache.getNativeCache().cleanUp();
        return cache.getNativeCache().estimatedSize();
    }

    private long populatedRegions() {
        return caches.allCaches().stream()
                .peek(c -> c.getNativeCache().cleanUp())
                .filter(c -> c.getNativeCache().estimatedSize() > 0)
                .count();
    }

    @Test
    void evictAll_clearsEveryRegion() {
        warmEveryRegion();
        assertThat(populatedRegions()).isEqualTo(12);

        Map<String, Object> result = endpoint.evict(null);

        assertThat(populatedRegions()).isZero();
        assertThat(result).containsEntry("evicted", "all");
    }

    /**
     * The reason this endpoint exists rather than {@code /actuator/caches}: the ETag must move, or a
     * client holding the old one is told (via 304) that its now-stale copy is still current.
     */
    @Test
    void evictAll_movesTheEtagSoNoStale304IsServed() {
        String before = versions.etagFor(AdministrativeAreaType.PARISH, Map.of());
        assertThat(before).isNotNull();

        endpoint.evict(null);

        String after = versions.etagFor(AdministrativeAreaType.PARISH, Map.of());
        assertThat(after).isNotNull().isNotEqualTo(before);
    }

    /**
     * A scoped eviction clears the level and everything below it, and NOTHING above — the same cascade
     * a write uses. PARISH is the leaf, so only its own two regions go.
     */
    @Test
    void evictByType_clearsOnlyThatCascade() {
        warmEveryRegion();

        Map<String, Object> result = endpoint.evict("PARISH");

        assertThat(l1Size(CacheValueKeyConfig.item(AdministrativeAreaType.PARISH))).isZero();
        assertThat(l1Size(CacheValueKeyConfig.search(AdministrativeAreaType.PARISH))).isZero();
        // A region two levels up is untouched.
        assertThat(l1Size(CacheValueKeyConfig.item(AdministrativeAreaType.REGION))).isEqualTo(1);
        assertThat(populatedRegions()).isEqualTo(10);
        assertThat(result).containsEntry("evicted", "PARISH");
    }

    @Test
    void evictByUnknownType_evictsNothingAndListsTheValidOnes() {
        warmEveryRegion();

        Map<String, Object> result = endpoint.evict("DISTRICT");   // not a real level name here

        assertThat(populatedRegions()).isEqualTo(12);
        assertThat(result).containsEntry("evicted", "none");
        assertThat(result.get("validTypes").toString()).contains("PARISH", "REGION");
    }

    @Test
    void state_reportsPerRegionSizes() {
        warmEveryRegion();

        Map<String, Object> state = endpoint.state();

        assertThat(state).containsEntry("l1EntriesTotal", 12L);
        @SuppressWarnings("unchecked")
        Map<String, Object> regions = (Map<String, Object>) state.get("regions");
        assertThat(regions).hasSize(12).containsValue(1L);
    }
}
