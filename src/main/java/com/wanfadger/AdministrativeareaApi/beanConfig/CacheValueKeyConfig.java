package com.wanfadger.AdministrativeareaApi.beanConfig;

/**
 * Centralized Redis cache-region names for the administrative-area resource.
 *
 * <p>Because this is a single type-dispatched resource, regions are operation-level
 * rather than per-entity: single full-detail reads and the paginated specification
 * search each get their own region. Every write (create/update/delete) evicts both so
 * stale lists never linger.
 *
 * <p>Mirrors the URRMS Core {@code CacheValueKeyConfig} convention.
 */
public interface CacheValueKeyConfig {

    /** Single full-detail reads (getOne) — 1 hour TTL. */
    String ADMINISTRATIVE_AREAS = "administrative_areas";

    /** Paginated {@code field:operator=value} search results (full hierarchy) — 30 min TTL. */
    String ADMINISTRATIVE_AREAS_SEARCH = "administrative_areas_search";
}
