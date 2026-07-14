package com.wanfadger.AdministrativeareaApi.cache;

import com.wanfadger.AdministrativeareaApi.beanConfig.CacheValueKeyConfig;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Cross-pod L1 invalidation over Redis pub/sub. Both ends live here: this pod publishes when it
 * writes, and listens for what the other pods publish.
 *
 * <h2>The origin check is what makes it safe</h2>
 *
 * Every message carries the id of the pod that sent it, and a pod ignores its own. Without that, the
 * writing pod would receive its own broadcast and clear caches it has already cleared — harmless in
 * isolation, but it turns one eviction into a second round of cold reads across the cluster.
 *
 * <h2>Received messages clear L1 only</h2>
 *
 * The pod that performed the write already evicted the shared L2. Every other pod has exactly one
 * stale copy to discard — its own, in its own heap. Calling the full {@code clear()} here would have
 * every pod in the cluster redundantly issue a Redis {@code SCAN}+{@code DEL} for keys that are
 * already gone: N pods doing the same delete N times.
 *
 * <h2>What this does not guarantee</h2>
 *
 * Redis pub/sub is fire-and-forget. There is no acknowledgement, no replay, and no delivery guarantee:
 * a pod that is mid-GC-pause, reconnecting, or simply not subscribed at that instant never sees the
 * message and no one finds out. That is the honest weakness of an in-heap tier, and it is why the L1
 * TTL is five minutes rather than thirty — the TTL is the backstop that bounds how long an unlucky pod
 * can be wrong. Making this reliable would mean a durable log (Redis Streams, Kafka), which is a large
 * amount of machinery to buy a few minutes of consistency on reference data that changes monthly.
 */
@Slf4j
@RequiredArgsConstructor
public class RedisCacheInvalidationBroadcaster implements CacheInvalidationBroadcaster, MessageListener {

    public static final String CHANNEL = "aa:cache:invalidate";

    /** Identifies this pod for the lifetime of the JVM. */
    private final String instanceId = UUID.randomUUID().toString();

    private final StringRedisTemplate redis;
    private final TwoLevelCacheManager cacheManager;

    @Override
    public void broadcast(AdministrativeAreaType type) {
        try {
            redis.convertAndSend(CHANNEL, instanceId + "|" + type.name());
        } catch (RuntimeException e) {
            // The local eviction has already happened and L2 is already cleared, so the data is not
            // wrong — other pods will simply take up to the L1 TTL to notice. Failing the write over
            // this would reject a legitimate database change because a cache hint could not be sent.
            log.warn("Could not broadcast cache invalidation for {} — other pods will catch up when "
                    + "their L1 expires ({}: {})", type, e.getClass().getSimpleName(), e.getMessage());
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        int separator = payload.indexOf('|');
        if (separator < 0) {
            log.warn("Ignoring malformed cache-invalidation message: {}", payload);
            return;
        }

        String origin = payload.substring(0, separator);
        if (instanceId.equals(origin)) {
            return;   // our own broadcast; we evicted before sending it
        }

        AdministrativeAreaType.fromStr(payload.substring(separator + 1)).ifPresentOrElse(
                type -> {
                    cacheManager.clearLocal(CacheValueKeyConfig.cascadeFrom(type));
                    log.debug("Cleared local L1 for {} and below, on behalf of pod {}", type, origin);
                },
                () -> log.warn("Ignoring cache-invalidation message for unknown type: {}", payload));
    }
}
