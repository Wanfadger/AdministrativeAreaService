package com.wanfadger.AdministrativeareaApi.service;

import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * The caching layer, as a separate bean in front of {@link AdministrativeAreaServiceImpl}.
 *
 * <pre>
 *   controller ─▶ AdministrativeAreaCachingService   @Cacheable      (no transaction)
 *                    └─▶ AdministrativeAreaServiceImpl   @Transactional  (no caching)
 * </pre>
 *
 * <h2>Why this is two beans and not two annotations on one method</h2>
 *
 * It used to be one method carrying both {@code @Cacheable} and {@code @Transactional}. Spring wraps
 * such a method in two independent AOP proxies, and their nesting order is decided by the advisors'
 * {@code order} — which for <b>both</b> defaults to {@code LOWEST_PRECEDENCE}. Ties are broken
 * arbitrarily. So the arrangement was not "cache outside transaction, as intended"; it was
 * "whichever the bean factory happened to sort first", and the transaction interceptor winning means
 * <b>every cache hit checks a connection out of the Hikari pool</b>, starts a transaction, finds the
 * value in the cache, and commits an empty transaction — burning the one resource that actually
 * limits this service's throughput, in order to do nothing.
 *
 * <p>Two beans make the nesting <b>structural</b>. The cache is outside the transaction because it is
 * in a different object, and no ordering configuration can change that. ({@code @EnableCaching(order =
 * HIGHEST_PRECEDENCE)} on the application class belts this braces, so that re-adding {@code @Cacheable}
 * to the transactional impl would still behave.)
 *
 * <h2>Constraint this imposes on everything downstream</h2>
 *
 * This bean runs <b>outside the transaction</b>. Entity→DTO mapping walks lazy associations, so it
 * must stay inside {@link AdministrativeAreaServiceImpl} where the session is open. With
 * {@code open-in-view=false}, hoisting mapping up here to "avoid mapping on a cache hit" would throw
 * {@code LazyInitializationException} — and only in production, since a test with an open session
 * would happily pass.
 */
@Service
@Primary
public class AdministrativeAreaCachingService implements AdministrativeAreaService {

    private final AdministrativeAreaService delegate;

    /**
     * Injected by bean name rather than by type — this bean is {@code @Primary} for the same
     * interface, so injecting by type would wire the facade into itself.
     */
    public AdministrativeAreaCachingService(
            @Qualifier("administrativeAreaServiceImpl") AdministrativeAreaService delegate) {
        this.delegate = delegate;
    }

    /**
     * {@code sync = true} is what routes reads through {@link com.wanfadger.AdministrativeareaApi.cache.TwoLevelCache#get(Object,
     * java.util.concurrent.Callable)} and gives single-flight loading: when a hot key expires, N
     * concurrent requests for it issue one query, not N.
     *
     * <p>It is only safe because that cache implements the sync path with striped {@link
     * java.util.concurrent.locks.ReentrantLock}s. Spring Data's stock {@code RedisCache} implements it
     * with a cache-wide {@code synchronized} block held across network I/O, which pins carrier threads
     * on this application's virtual-thread runtime — see that class for the full explanation. Turning
     * this flag on over a plain {@code RedisCacheManager} would be a throughput regression wearing the
     * costume of an optimisation.
     *
     * <p>The region is chosen at runtime from the {@code type} in the query map — {@code cacheNames}
     * cannot express that, which is why there is a resolver.
     */
    @Override
    @Cacheable(cacheResolver = "areaTypeCacheResolver", keyGenerator = "searchKeyGenerator", sync = true)
    public ResponseDTO<AdministrativeAreaDTO> getOne(Map<String, String> queryMap) {
        return delegate.getOne(queryMap);
    }

    @Override
    @Cacheable(cacheResolver = "areaTypeCacheResolver", keyGenerator = "searchKeyGenerator", sync = true)
    public PaginatedResponseDTO<AdministrativeAreaDTO> search(Map<String, String> queryMap) {
        return delegate.search(queryMap);
    }

    // ---------------------------------------------------------------- writes
    //
    // No cache annotations. Eviction is scoped to the affected levels and must run *after the
    // transaction commits*, and @CacheEvict can express neither — see AreaCacheInvalidator, which the
    // transactional impl drives from inside the transaction.

    @Override
    public ResponseDTO<List<String>> create(Map<String, String> queryMap, List<NewAdministrativeAreaDTO> dtos) {
        return delegate.create(queryMap, dtos);
    }

    @Override
    public ResponseDTO<String> updateOne(Map<String, String> queryMap, NewAdministrativeAreaDTO dto) {
        return delegate.updateOne(queryMap, dto);
    }

    @Override
    public ResponseDTO<String> delete(Map<String, String> queryMap) {
        return delegate.delete(queryMap);
    }
}
