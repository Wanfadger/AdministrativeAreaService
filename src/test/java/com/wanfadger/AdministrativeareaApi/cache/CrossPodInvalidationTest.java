package com.wanfadger.AdministrativeareaApi.cache;

import com.wanfadger.AdministrativeareaApi.beanConfig.CacheValueKeyConfig;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Two simulated pods sharing one Redis: a write on A must clear B's in-heap tier, and must not clear
 * A's a second time.
 *
 * <p>Rather than mock the broadcaster, this wires two real {@link RedisCacheInvalidationBroadcaster}s
 * — each with its own cache manager, as two pods would have — and pipes what either publishes into
 * both, which is exactly what Redis pub/sub does. The behaviour under test is the origin check, and a
 * mock would have asserted my assumption about it rather than the thing itself.
 */
class CrossPodInvalidationTest {

    private TwoLevelCacheManager podACaches;
    private TwoLevelCacheManager podBCaches;
    private RedisCacheInvalidationBroadcaster podA;
    private RedisCacheInvalidationBroadcaster podB;

    /** Every message published by either pod is delivered to both — Redis pub/sub semantics. */
    private final List<String> published = new ArrayList<>();

    @BeforeEach
    void twoPodsOnOneRedis() {
        AreaCacheProperties props = new AreaCacheProperties();
        podACaches = new TwoLevelCacheManager(null, props);
        podBCaches = new TwoLevelCacheManager(null, props);

        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        podA = new RedisCacheInvalidationBroadcaster(redis, podACaches);
        podB = new RedisCacheInvalidationBroadcaster(redis, podBCaches);

        doAnswer(call -> {
            String payload = call.getArgument(1);
            published.add(payload);
            deliver(payload, podA);
            deliver(payload, podB);
            return null;
        }).when(redis).convertAndSend(anyString(), anyString());
    }

    private void deliver(String payload, RedisCacheInvalidationBroadcaster pod) {
        pod.onMessage(new DefaultMessage(
                RedisCacheInvalidationBroadcaster.CHANNEL.getBytes(StandardCharsets.UTF_8),
                payload.getBytes(StandardCharsets.UTF_8)), null);
    }

    private void warm(TwoLevelCacheManager pod) {
        pod.allCaches().forEach(cache -> cache.put("k", "cached-value"));
    }

    private long populatedRegions(TwoLevelCacheManager pod) {
        return pod.allCaches().stream()
                .peek(cache -> cache.getNativeCache().cleanUp())
                .filter(cache -> cache.getNativeCache().estimatedSize() > 0)
                .count();
    }

    /** The point of the whole mechanism. */
    @Test
    void aWriteOnPodA_clearsPodBsInHeapTier() {
        warm(podACaches);
        warm(podBCaches);
        assertThat(populatedRegions(podBCaches)).isEqualTo(12);

        // Pod A handles the write: it evicts locally, then announces.
        podACaches.cascadeFrom(AdministrativeAreaType.PARISH).forEach(TwoLevelCache::clear);
        podA.broadcast(AdministrativeAreaType.PARISH);

        assertThat(populatedRegions(podBCaches))
                .as("pod B must drop its two now-stale parish regions")
                .isEqualTo(10);
    }

    /** The cascade travels with the message, not just the level that was written. */
    @Test
    void aRegionWriteOnPodA_clearsEveryLevelOnPodB() {
        warm(podBCaches);

        podA.broadcast(AdministrativeAreaType.REGION);

        assertThat(populatedRegions(podBCaches))
                .as("every level embeds the region, so every level on pod B is now stale")
                .isZero();
    }

    /**
     * A pod ignores its own broadcast. Without the origin check the writer would clear caches it has
     * already cleared — and, worse, would clear anything it had legitimately re-cached in the interval
     * between evicting and receiving its own message back.
     */
    @Test
    void aPodIgnoresItsOwnBroadcast() {
        podA.broadcast(AdministrativeAreaType.PARISH);   // pod A evicted already; this is the announcement

        // Whatever pod A re-caches after evicting must survive its own message coming back round.
        warm(podACaches);
        deliver(published.get(0), podA);

        assertThat(populatedRegions(podACaches))
                .as("pod A must not act on its own invalidation")
                .isEqualTo(12);
    }

    @Test
    void aMalformedMessageIsIgnored_notFatal() {
        warm(podBCaches);

        deliver("garbage-with-no-separator", podB);
        deliver("some-pod-id|NOT_A_REAL_LEVEL", podB);

        assertThat(populatedRegions(podBCaches)).isEqualTo(12);
    }

    /** The message names the level; the receiver derives the cascade from it. */
    @Test
    void theBroadcastCarriesTheOriginAndTheLevel() {
        podA.broadcast(AdministrativeAreaType.COUNTY);

        assertThat(published).singleElement().satisfies(payload -> {
            assertThat(payload).endsWith("|COUNTY");
            assertThat(payload.split("\\|")[0]).as("origin id").isNotBlank();
        });
        assertThat(CacheValueKeyConfig.cascadeFrom(AdministrativeAreaType.COUNTY))
                .hasSize(6);   // county + subcounty + parish, item and search each
    }
}
