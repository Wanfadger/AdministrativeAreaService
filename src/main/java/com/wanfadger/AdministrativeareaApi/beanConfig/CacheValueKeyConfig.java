package com.wanfadger.AdministrativeareaApi.beanConfig;

import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The cache region names — <b>twelve</b> of them: an item region and a search region for each of the
 * six levels.
 *
 * <p>This used to be two regions shared by all six levels ({@code administrative_areas} and
 * {@code administrative_areas_search}), which made precise eviction impossible: with every level's
 * entries mixed into one region, the only way to invalidate a region rename was
 * {@code @CacheEvict(allEntries = true)}, and that threw away the other five levels too. Splitting by
 * type is what lets a parish write clear the two parish regions and leave the other ten standing —
 * see {@link AdministrativeAreaType#selfAndDescendants()}.
 *
 * <p>Two things fall out of the split for free: {@code clear()} on a per-type region is a cheap,
 * bounded operation rather than a scan of everything, and Micrometer reports a hit rate <i>per level</i>,
 * so "the cache is at 95%" can be interrogated rather than believed.
 *
 * <p>Names are used as Redis key prefixes ({@code aa_search_PARISH::{page:1, ...}}), so they are
 * deliberately readable in RedisInsight.
 */
public final class CacheValueKeyConfig {

    private CacheValueKeyConfig() {
    }

    private static final String ITEM_PREFIX = "aa_item_";
    private static final String SEARCH_PREFIX = "aa_search_";

    /** Single full-detail reads ({@code getOne}) for one level. */
    public static String item(AdministrativeAreaType type) {
        return ITEM_PREFIX + type.name();
    }

    /** Paginated search results for one level. */
    public static String search(AdministrativeAreaType type) {
        return SEARCH_PREFIX + type.name();
    }

    /**
     * All twelve region names. Declared to the cache manager up front rather than created lazily on
     * first use — a lazily-created region does not exist at startup, so Micrometer has nothing to
     * bind to and its hit rate is simply absent from the dashboard until someone happens to query
     * that level. That is exactly how a cache ends up "unmonitored but fine".
     */
    public static Set<String> allCacheNames() {
        Set<String> names = new LinkedHashSet<>();
        for (AdministrativeAreaType type : AdministrativeAreaType.values()) {
            names.add(item(type));
            names.add(search(type));
        }
        return names;
    }

    /** The regions a write to {@code type} must clear: its own, and every level below it. */
    public static Set<String> cascadeFrom(AdministrativeAreaType type) {
        Set<String> names = new LinkedHashSet<>();
        for (AdministrativeAreaType affected : type.selfAndDescendants()) {
            names.add(item(affected));
            names.add(search(affected));
        }
        return names;
    }

    /** The {@link AdministrativeAreaType} a region name belongs to. */
    public static AdministrativeAreaType typeOf(String cacheName) {
        String suffix = cacheName.startsWith(ITEM_PREFIX)
                ? cacheName.substring(ITEM_PREFIX.length())
                : cacheName.substring(SEARCH_PREFIX.length());
        return Arrays.stream(AdministrativeAreaType.values())
                .filter(t -> t.name().equals(suffix))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Not a cache region: " + cacheName));
    }

    /** True for the paginated-search regions, whose entries hold a whole page of rows. */
    public static boolean isSearchRegion(String cacheName) {
        return cacheName.startsWith(SEARCH_PREFIX);
    }
}
