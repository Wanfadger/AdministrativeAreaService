package com.wanfadger.AdministrativeareaApi.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A monotonically increasing version per administrative level, used to build ETags.
 *
 * <p>The version answers one question cheaply: <i>has anything at this level changed since the
 * client last asked?</i> If not, the request can be answered with a 304 <b>without calling the
 * service at all</b> — no query, no DTO mapping, no JSON serialization.
 *
 * <h2>Why not Spring's ShallowEtagHeaderFilter</h2>
 *
 * Because it saves bandwidth and nothing else. It lets the request run to completion, renders the
 * full response body, MD5-hashes it, compares, and then <i>throws the body away</i> and returns 304.
 * The database was still queried and 300KB of JSON was still built and hashed. On the hot path this
 * codebase cares about, that is most of the cost, and the filter saves none of it.
 *
 * <h2>Where the version lives, and why it is not just a field</h2>
 *
 * In Redis, as {@code aa:ver:<TYPE>}, incremented on write.
 *
 * <p>A per-pod counter would be actively dangerous. Pod A takes a write and bumps its counter; pod B
 * never hears about it and keeps returning the old version — so a client holding the old ETag asks
 * pod B, gets a <b>304</b>, and keeps stale data believing it is current. A stale 304 is worse than a
 * stale cache: the client cannot tell, and there is nothing to expire. Piggybacking on the pub/sub
 * invalidation would not fix it either, because that channel is explicitly best-effort — a pod that
 * misses the message would keep serving stale 304s indefinitely, long after its cache had healed.
 *
 * <p>So the version is read from Redis, which is shared and authoritative. To keep that off the hot
 * path it is memoised locally for {@link #VERSION_TTL} — one {@code GET} every two seconds per level,
 * not one per request. The cost is a bounded staleness window: for up to two seconds after a write,
 * another pod may still hand out a 304 against the previous version. That is a hard bound, not a hope.
 *
 * <h2>When Redis is unavailable, there are no ETags</h2>
 *
 * Deliberately. Without the shared counter this pod cannot know whether its version is current, and
 * the failure mode of guessing is a stale 304. So {@link #etagFor} returns null, no ETag header is
 * sent, and every request is answered in full — slower, and correct. (A single-instance deployment,
 * {@code app.cache.l2-enabled=false}, is different: there its local counter <i>is</i> authoritative,
 * so ETags stay on.)
 */
@Component
@Slf4j
public class AreaVersionRegistry {

    private static final String KEY_PREFIX = "aa:ver:";

    /** How long a version read from Redis is trusted before being re-read. */
    private static final Duration VERSION_TTL = Duration.ofSeconds(2);

    private final ObjectProvider<StringRedisTemplate> redis;
    private final boolean l2Enabled;

    /** Authoritative only when there is no L2 (single instance); otherwise just a fallback seed. */
    private final Map<AdministrativeAreaType, AtomicLong> localVersions =
            new EnumMap<>(AdministrativeAreaType.class);

    /** Memoised Redis reads, so the hot path does not make a network call per request. */
    private final com.github.benmanes.caffeine.cache.Cache<AdministrativeAreaType, Long> memo =
            Caffeine.newBuilder().expireAfterWrite(VERSION_TTL).build();

    public AreaVersionRegistry(ObjectProvider<StringRedisTemplate> redis, AreaCacheProperties props) {
        this.redis = redis;
        this.l2Enabled = props.isL2Enabled();
        for (AdministrativeAreaType type : AdministrativeAreaType.values()) {
            localVersions.put(type, new AtomicLong());
        }
    }

    /**
     * Bump {@code type} and every level below it — the same cascade the cache eviction uses, and for
     * the same reason: every DTO embeds its ancestry, so renaming a region changes the bytes of every
     * parish, and every parish ETag must therefore change too.
     *
     * <p>Called after commit, alongside the eviction.
     */
    public void bump(AdministrativeAreaType type) {
        StringRedisTemplate template = template();
        for (AdministrativeAreaType affected : type.selfAndDescendants()) {
            localVersions.get(affected).incrementAndGet();
            if (template != null) {
                try {
                    template.opsForValue().increment(KEY_PREFIX + affected.name());
                } catch (RuntimeException e) {
                    log.warn("Could not bump the Redis version for {} — ETags will be withheld until "
                            + "Redis recovers, which is the safe direction ({})", affected, e.getMessage());
                }
            }
            memo.invalidate(affected);   // this pod must not serve the old version for the next 2s
        }
    }

    /**
     * The ETag for a canonical query, or {@code null} when no ETag can be trusted.
     *
     * <p>Two requests get the same ETag only if they are the same query at the same version, so the
     * canonical query map has to be part of it: without that, {@code size=50} and {@code size=2000}
     * would share an ETag and clients would be told their wrong-sized page was still valid.
     */
    @Nullable
    public String etagFor(AdministrativeAreaType type, Map<String, String> canonicalQuery) {
        Long version = version(type);
        if (version == null) {
            return null;   // Redis unreachable: an ETag here could only be a guess, and a wrong guess
        }                  // is a stale 304 that the client can never detect
        return "\"" + type.name() + '-' + version + '-'
                + Integer.toHexString(canonicalQuery.hashCode()) + "\"";
    }

    /** Current version, or null if it cannot be established. */
    @Nullable
    private Long version(AdministrativeAreaType type) {
        if (!l2Enabled) {
            return localVersions.get(type).get();   // single instance: the local counter IS the truth
        }
        return memo.get(type, this::readFromRedis);
    }

    @Nullable
    private Long readFromRedis(AdministrativeAreaType type) {
        StringRedisTemplate template = template();
        if (template == null) {
            return null;
        }
        try {
            String raw = template.opsForValue().get(KEY_PREFIX + type.name());
            // Absent simply means nothing has been written since this Redis was created. Version 0.
            return raw == null ? 0L : Long.parseLong(raw);
        } catch (RuntimeException e) {   // covers a Redis failure and a non-numeric value alike
            log.warn("Could not read the version for {} — withholding ETags ({})", type, e.getMessage());
            return null;
        }
    }

    @Nullable
    private StringRedisTemplate template() {
        return redis.getIfAvailable();
    }
}
