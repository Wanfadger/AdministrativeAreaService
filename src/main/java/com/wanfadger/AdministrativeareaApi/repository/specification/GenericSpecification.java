package com.wanfadger.AdministrativeareaApi.repository.specification;

import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Reusable JPA Specification driven by a {@link SearchCriteria} triple
 * (key, value, matchType). Mirrors the URRMS Core GenericSpecification so
 * cross-service search ergonomics stay identical.
 *
 * <p>Unknown attribute names are tolerated — the caller never sees the failure,
 * the offending criterion is simply dropped.
 */
@Slf4j
public class GenericSpecification<T> implements Specification<T> {

    private final SearchCriteria criteria;

    public GenericSpecification(SearchCriteria criteria) {
        this.criteria = criteria;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        if (criteria.getValue() == null) return null;
        try {
            return buildPredicate(root, query, builder);
        } catch (RuntimeException ex) {
            log.warn("Skipping unknown / unfilterable field '{}' on entity {}: {}",
                    criteria.getKey(), root.getJavaType().getSimpleName(), ex.getMessage());
            return builder.conjunction();
        }
    }

    private Predicate buildPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        String key = criteria.getKey();
        String stringValue = criteria.getValue().toString().toLowerCase();
        Object rawValue = criteria.getValue();
        MatchType matchType = criteria.getMatchType() == null ? MatchType.EQUALS : criteria.getMatchType();

        Path<?> path = getPath(root, key, query);
        Class<?> javaType = path.getJavaType();

        switch (matchType) {
            case EQUALS:
                return builder.equal(path, castToRequiredType(javaType, rawValue.toString()));
            case NOT_EQUALS:
                return builder.notEqual(path, castToRequiredType(javaType, rawValue.toString()));
            case CONTAINS:
                if (javaType == String.class) {
                    return builder.like(builder.lower(path.as(String.class)), "%" + stringValue + "%");
                }
                return builder.equal(path, castToRequiredType(javaType, rawValue.toString()));
            case NOT_CONTAINS:
                if (javaType == String.class) {
                    return builder.notLike(builder.lower(path.as(String.class)), "%" + stringValue + "%");
                }
                return builder.notEqual(path, castToRequiredType(javaType, rawValue.toString()));
            case GT:
                if (Number.class.isAssignableFrom(javaType)) {
                    return builder.gt(path.as(Number.class),
                            (Number) castToRequiredType(javaType, rawValue.toString()));
                }
                return builder.greaterThan(path.as(String.class), rawValue.toString());
            case LT:
                if (Number.class.isAssignableFrom(javaType)) {
                    return builder.lt(path.as(Number.class),
                            (Number) castToRequiredType(javaType, rawValue.toString()));
                }
                return builder.lessThan(path.as(String.class), rawValue.toString());
            case GTE:
                if (Number.class.isAssignableFrom(javaType)) {
                    return builder.ge(path.as(Number.class),
                            (Number) castToRequiredType(javaType, rawValue.toString()));
                }
                return builder.greaterThanOrEqualTo(path.as(String.class), rawValue.toString());
            case LTE:
                if (Number.class.isAssignableFrom(javaType)) {
                    return builder.le(path.as(Number.class),
                            (Number) castToRequiredType(javaType, rawValue.toString()));
                }
                return builder.lessThanOrEqualTo(path.as(String.class), rawValue.toString());
            case IN: {
                // Split CSV → typed collection so JPA emits `field IN (v1, v2, …)`.
                String raw = rawValue.toString();
                List<Object> values = new ArrayList<>();
                for (String token : Arrays.asList(raw.split(","))) {
                    String trimmed = token.trim();
                    if (trimmed.isEmpty()) continue;
                    values.add(castToRequiredType(javaType, trimmed));
                }
                if (values.isEmpty()) return builder.disjunction();
                return path.in(values);
            }
            default:
                return null;
        }
    }

    private Path<?> getPath(Root<T> root, String attributeName, CriteriaQuery<?> query) {
        if (!attributeName.contains(".")) {
            return root.get(attributeName);
        }
        String[] parts = attributeName.split("\\.");
        From<?, ?> from = root;
        query.distinct(true);
        for (int i = 0; i < parts.length - 1; i++) {
            from = from.join(parts[i], JoinType.LEFT);
        }
        return from.get(parts[parts.length - 1]);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object castToRequiredType(Class<?> fieldType, String value) {
        if (fieldType.isAssignableFrom(Double.class)) {
            return Double.valueOf(value);
        } else if (fieldType.isAssignableFrom(Integer.class)) {
            return Integer.valueOf(value);
        } else if (fieldType.isAssignableFrom(Long.class)) {
            return Long.valueOf(value);
        } else if (fieldType.isAssignableFrom(Boolean.class) || fieldType.isAssignableFrom(boolean.class)) {
            return Boolean.valueOf(value);
        } else if (UUID.class.isAssignableFrom(fieldType)) {
            return UUID.fromString(value);
        } else if (Enum.class.isAssignableFrom(fieldType)) {
            Class<? extends Enum> enumType = (Class<? extends Enum>) fieldType;
            try {
                return Enum.valueOf(enumType, value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return Enum.valueOf(enumType, value);
            }
        }
        return value;
    }
}
