package com.wanfadger.AdministrativeareaApi.cache;

import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;

/**
 * Tells the <i>other</i> pods that a level has changed.
 *
 * <p>The L2 tier needs no help — it is shared, and the pod that performed the write already evicted
 * it. The problem is L1: it lives in each pod's heap, so every pod that did not handle the write is
 * still holding its own copy of data that is now wrong, and has no reason to go and look at Redis
 * again.
 *
 * <p>Without this, that staleness is bounded only by the L1 TTL — five minutes of some pods serving
 * an old name while others serve the new one, depending on which pod the load balancer picked. This
 * shrinks the window to milliseconds.
 *
 * <p>An interface rather than a class because the Redis implementation cannot exist without Redis:
 * single-instance deployments and the test profile get {@link #NOOP}, which is correct rather than
 * degraded — with one pod there is no other L1 to invalidate.
 */
public interface CacheInvalidationBroadcaster {

    /** Announce that {@code type} (and, by implication, every level below it) has changed. */
    void broadcast(AdministrativeAreaType type);

    /** For deployments with nothing to broadcast to. */
    CacheInvalidationBroadcaster NOOP = type -> {
    };
}
