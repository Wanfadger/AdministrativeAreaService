package com.wanfadger.AdministrativeareaApi.cache;

import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * Evicts the caches a write invalidates — its own level and every level below it.
 *
 * <p>Replaces {@code @CacheEvict(allEntries = true)} on both regions, which discarded all six levels
 * on every write. This is the same operation done twice as precisely: <b>which</b> regions (see
 * {@link AdministrativeAreaType#selfAndDescendants()}) and <b>when</b>.
 *
 * <h2>The "when" is the part that was actually broken</h2>
 *
 * {@code @CacheEvict} without {@code beforeInvocation} fires when the method returns — which, for a
 * {@code @Transactional} service method, is <b>before the transaction commits</b>. That leaves a
 * window: the cache is empty, the new row is not yet visible to other connections, and a concurrent
 * reader that arrives inside that window loads the <i>pre-commit</i> state and caches it. The write
 * then commits, and nothing evicts again. The cache is now permanently stale — until the TTL expires,
 * which for the item regions was an hour. It is a narrow window and an intermittent, unreproducible
 * bug, which is the worst kind.
 *
 * <p>Registering as an {@code afterCommit} synchronisation closes it: the eviction cannot run before
 * the data it is invalidating is visible.
 *
 * <p>Deliberately <b>not</b> {@code afterCompletion}: on rollback there is nothing to invalidate,
 * because nothing changed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AreaCacheInvalidator {

    private final TwoLevelCacheManager cacheManager;

    /**
     * {@link CacheInvalidationBroadcaster#NOOP} on a single instance and under the test profile —
     * with one pod there is no other L1 to invalidate, so there is nothing to announce.
     */
    private final CacheInvalidationBroadcaster broadcaster;

    /** Drives the ETags. Bumped in lockstep with the eviction, and for the same cascade. */
    private final AreaVersionRegistry versions;

    /**
     * Schedule eviction of {@code type} and everything below it, to run once the current transaction
     * commits. Called once per write operation — a bulk create of 500 parishes registers one
     * synchronisation, not 500.
     */
    public void invalidate(AdministrativeAreaType type) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evictNow(type);
                }
            });
        } else {
            // No transaction in progress (a direct call, or a test): nothing to wait for.
            evictNow(type);
        }
    }

    /**
     * Evict <b>every</b> region across the whole cluster, immediately. For operational use — a bad
     * deploy, a manual database edit, a suspected poisoned entry — not the write path, which uses the
     * scoped {@link #invalidate(AdministrativeAreaType)} above.
     *
     * <p>Does the three things a correct cluster-wide eviction must do, and that the built-in
     * {@code /actuator/caches} endpoint does not:
     * <ol>
     *   <li>clears L1 <b>and</b> L2 on this pod, for all twelve regions;</li>
     *   <li>bumps every version, so ETags move — otherwise a client holding an old {@code If-None-Match}
     *       gets a {@code 304} against freshly-loaded data and never sees the change, which is worse
     *       than a stale cache because nothing will expire it;</li>
     *   <li>broadcasts, so the other pods drop their in-heap copies too instead of serving them for up
     *       to the L1 TTL.</li>
     * </ol>
     *
     * <p>Runs synchronously and outside any transaction: an operator issuing this wants it to have
     * happened when the call returns, not on some later commit.
     */
    public void invalidateAll() {
        cacheManager.allCaches().forEach(TwoLevelCache::clear);

        // The root's cascade IS every level (selfAndDescendants of the first ordinal = all types), so a
        // root bump moves every version and a root broadcast clears every other pod's entire L1.
        AdministrativeAreaType root = AdministrativeAreaType.values()[0];
        versions.bump(root);
        broadcaster.broadcast(root);

        log.info("Evicted ALL {} cache regions cluster-wide on operator request", cacheManager.getCacheNames().size());
    }

    private void evictNow(AdministrativeAreaType type) {
        List<TwoLevelCache> affected = cacheManager.cascadeFrom(type);
        affected.forEach(TwoLevelCache::clear);

        // Bump BEFORE announcing, for the same reason the eviction comes first: the moment other pods
        // hear about this write they will start answering conditional requests again, and they must
        // not still be validating against the old version.
        versions.bump(type);

        // Evict first, announce second. The other pods clear their L1 on this message and will
        // immediately re-read; if L2 had not been cleared yet, they would re-cache the stale value
        // straight back out of Redis and the invalidation would have achieved nothing.
        broadcaster.broadcast(type);

        if (log.isDebugEnabled()) {
            log.debug("Write to {} evicted {} of {} regions: {}",
                    type, affected.size(), cacheManager.getCacheNames().size(),
                    affected.stream().map(TwoLevelCache::getName).toList());
        }
    }
}
