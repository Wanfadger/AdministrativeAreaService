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
