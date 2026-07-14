package com.wanfadger.AdministrativeareaApi.cache;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Starts the pub/sub subscription, and keeps trying if Redis is not there yet.
 *
 * <h2>Why this exists</h2>
 *
 * {@link RedisMessageListenerContainer} starts eagerly with the application context and <b>throws if
 * its initial SUBSCRIBE fails</b> — which aborts the context. So simply declaring the container meant
 * that <b>Redis being down at boot stopped the application from starting at all</b>, exactly negating
 * the property the two-tier cache was built to have: that Redis is an optimisation the service can
 * live without. A pod that refuses to start because a <i>cache</i> is unavailable is worse than a pod
 * that starts slow.
 *
 * <p>(This was not theoretical. It bit during a routine restart: a 250ms connect timeout, briefly
 * exceeded on a loaded host, took the whole application down with
 * {@code Connection initialization timed out}.)
 *
 * <p>So the container is declared with {@code autoStartup=false} and started from here, after the
 * application is up and serving. A failure is logged and retried in the background; the service runs
 * in the meantime, correctly, with the L1 TTL as its staleness bound instead of pub/sub. Once the
 * subscription is established, the container handles reconnection itself.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationSubscriber {

    private static final long RETRY_SECONDS = 10;

    private final RedisMessageListenerContainer container;

    private final ScheduledExecutorService retries = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "cache-invalidation-subscribe-retry");
        thread.setDaemon(true);   // must never hold the JVM open
        return thread;
    });

    /**
     * After the app is serving, not during startup — the point is that this cannot delay or block
     * readiness.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void subscribe() {
        attempt();
    }

    /** Set only on a genuinely established subscription. See why {@code isRunning()} cannot be used. */
    private final java.util.concurrent.atomic.AtomicBoolean subscribed =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    private void attempt() {
        if (subscribed.get()) {
            return;
        }
        try {
            // A start() that threw still leaves the container reporting isRunning() == true, with no
            // subscription behind it. Guarding on isRunning() therefore made every retry return
            // immediately — the first failure was permanent, and silently so: cross-pod invalidation
            // stayed off for the life of the pod while the container cheerfully claimed to be running.
            // Reset the half-started state before trying again.
            if (container.isRunning()) {
                container.stop();
            }
            container.start();
            subscribed.set(true);
            log.info("Subscribed to '{}' — cross-pod cache invalidation is active",
                    RedisCacheInvalidationBroadcaster.CHANNEL);
        } catch (RuntimeException e) {
            log.warn("Could not subscribe to Redis for cross-pod cache invalidation ({}: {}). "
                            + "The service is running normally; until this succeeds, a stale L1 on another "
                            + "pod is bounded by its TTL rather than cleared on write. Retrying in {}s.",
                    e.getClass().getSimpleName(), e.getMessage(), RETRY_SECONDS);
            retries.schedule(this::attempt, RETRY_SECONDS, TimeUnit.SECONDS);
        }
    }

    @PreDestroy
    void shutdown() {
        retries.shutdownNow();
    }
}
