package com.wanfadger.AdministrativeareaApi.repository.specification;

import org.springframework.data.jpa.domain.Specification;

import java.util.Map;
import java.util.Set;

/**
 * Turns a controller {@code Map<String,String> queryMap} into a JPA
 * {@link Specification} using the {@code field[:operator]=value} convention,
 * AND-composing one {@link GenericSpecification} per filter entry.
 *
 * <p>Mirrors the URRMS Core {@code SpecificationBuilder} (plus the {@link #freeText}
 * helper) so cross-service search ergonomics stay identical. Unknown operators fall
 * back to {@link MatchType#EQUALS}; blank values are skipped.
 */
public final class SpecificationBuilder {

    /** Paging / sort / free-text keys that never become filter criteria. */
    private static final Set<String> RESERVED_QUERY_KEYS =
            Set.of("page", "size", "sortBy", "sortDirection", "search");

    private SpecificationBuilder() {
    }

    /**
     * AND-composes a {@code Specification<T>} from the {@code field[:operator]=value}
     * entries in {@code queryMap}. Skips the reserved paging/sort/{@code search}
     * keys and any caller-supplied {@code handledKeys} (e.g. {@code type},
     * {@code detailed}) that drive dedicated branches.
     *
     * @param queryMap    the raw request params
     * @param handledKeys base field names already consumed by the caller (may be empty/null)
     */
    public static <T> Specification<T> fromQueryMap(Map<String, String> queryMap, Set<String> handledKeys) {
        Specification<T> spec = (root, query, cb) -> cb.conjunction();
        if (queryMap == null) {
            return spec;
        }

        for (Map.Entry<String, String> entry : queryMap.entrySet()) {
            String rawKey = entry.getKey();
            String value = entry.getValue();
            if (rawKey == null || value == null || value.isBlank()) {
                continue;
            }

            int colon = rawKey.indexOf(':');
            String field = colon > 0 ? rawKey.substring(0, colon) : rawKey;

            if (RESERVED_QUERY_KEYS.contains(field)
                    || (handledKeys != null && handledKeys.contains(field))) {
                continue;
            }

            MatchType matchType = MatchType.EQUALS;
            if (colon > 0) {
                try {
                    matchType = MatchType.valueOf(rawKey.substring(colon + 1).toUpperCase());
                } catch (IllegalArgumentException ignored) {
                    // unknown operator → keep default EQUALS
                }
            }

            spec = spec.and(new GenericSpecification<>(new SearchCriteria(field, value, matchType)));
        }

        return spec;
    }

    /** Single-criterion convenience for composing explicit predicates in services. */
    public static <T> Specification<T> where(String field, Object value, MatchType matchType) {
        return new GenericSpecification<>(new SearchCriteria(field, value, matchType));
    }

    /**
     * Free-text {@code search} predicate: case-insensitive {@code CONTAINS} OR-ed
     * across the given text fields (e.g. name/code/description). Returns an
     * AND-neutral (always-true) spec when {@code search} is blank or no fields are
     * supplied, so callers can unconditionally {@code .and(freeText(...))}.
     */
    public static <T> Specification<T> freeText(String search, String... fields) {
        if (search == null || search.isBlank() || fields == null || fields.length == 0) {
            return (root, query, cb) -> cb.conjunction();
        }
        Specification<T> spec = null;
        for (String field : fields) {
            Specification<T> like = where(field, search, MatchType.CONTAINS);
            spec = (spec == null) ? like : spec.or(like);
        }
        return spec;
    }
}
