package com.wanfadger.AdministrativeareaApi.beanConfig;

/**
 * Centralized Redis cache-region names for the administrative-area resource.
 *
 * <p>Because this is a single type-dispatched resource, regions are operation-level
 * rather than per-entity: light {@code code+name} reads, full-detail reads, and the
 * paginated specification search each get their own region. Every write
 * (create/update/upload/delete) evicts all three so stale lists never linger.
 *
 * <p>Mirrors the URRMS Core {@code CacheValueKeyConfig} convention.
 */
public interface CacheValueKeyConfig {

    /** Full-detail reads (searchOne/searchList) — 1 hour TTL. */
    String ADMINISTRATIVE_AREAS = "administrative_areas";

    /** Light code+name reads (filterOne/filterList/parishes) — 7 day TTL. */
    String ADMINISTRATIVE_AREAS_FILTER = "administrative_areas_filter";

    /** Paginated {@code field:operator=value} search results — 30 min TTL. */
    String ADMINISTRATIVE_AREAS_SEARCH = "administrative_areas_search";
}
