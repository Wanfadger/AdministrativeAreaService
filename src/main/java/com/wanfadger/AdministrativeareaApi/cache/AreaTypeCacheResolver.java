package com.wanfadger.AdministrativeareaApi.cache;

import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.MissingDataException;
import com.wanfadger.AdministrativeareaApi.beanConfig.CacheValueKeyConfig;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Picks the cache region for a read from the {@code type} inside the query map.
 *
 * <p>{@code @Cacheable(cacheNames = ...)} cannot express this: its names are compile-time constants,
 * and the region here depends on a runtime argument. That constraint is precisely why the old code
 * had one region for all six levels — and therefore why its only available eviction was
 * {@code allEntries = true}.
 *
 * <p>Which of the two regions is chosen depends on the method: {@code getOne} reads single items,
 * everything else reads pages.
 */
@Component
@RequiredArgsConstructor
public class AreaTypeCacheResolver implements CacheResolver {

    private final CacheManager cacheManager;

    @Override
    public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
        AdministrativeAreaType type = typeOf(context);

        String region = "getOne".equals(context.getMethod().getName())
                ? CacheValueKeyConfig.item(type)
                : CacheValueKeyConfig.search(type);

        Cache cache = cacheManager.getCache(region);
        if (cache == null) {
            // Unreachable unless a level is added to the enum without rebuilding the regions.
            throw new IllegalStateException("No cache region '" + region + "'");
        }
        // Exactly one cache: @Cacheable(sync = true) rejects a resolver that returns more.
        return List.of(cache);
    }

    private AdministrativeAreaType typeOf(CacheOperationInvocationContext<?> context) {
        Object[] args = context.getArgs();
        if (args.length > 0 && args[0] instanceof Map<?, ?> queryMap) {
            Object type = queryMap.get("type");
            if (type != null) {
                return AdministrativeAreaType.fromStr(String.valueOf(type))
                        .orElseThrow(AreaTypeCacheResolver::missingType);
            }
        }
        // Thrown from inside the caching aspect rather than from the service, but it is the same
        // exception with the same message, so the response is byte-for-byte what it was before.
        throw missingType();
    }

    private static MissingDataException missingType() {
        return new MissingDataException("Missing Administrative Area Type");
    }
}
