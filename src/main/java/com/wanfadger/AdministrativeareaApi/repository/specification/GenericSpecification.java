package com.wanfadger.AdministrativeareaApi.repository.specification;

import com.wanfadger.AdministrativeareaApi.dto.SearchCriteria;
import com.wanfadger.AdministrativeareaApi.enums.MatchType;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GenericSpecification<T> implements Specification<T> {

    private final SearchCriteria criteria;

    public GenericSpecification(SearchCriteria criteria) {
        this.criteria = criteria;
    }

    @Override
    public Predicate toPredicate(@org.springframework.lang.NonNull Root<T> root,
            @org.springframework.lang.NonNull CriteriaQuery<?> query,
            @org.springframework.lang.NonNull CriteriaBuilder builder) {
        if (criteria.getValue() == null)
            return null;

        String key = criteria.getKey();
        Object rawValue = criteria.getValue();
        String stringValue = rawValue.toString().toLowerCase();

        MatchType matchType = criteria.getMatchType() == null ? MatchType.EQUALS : criteria.getMatchType();

        // Get Path (handles joins for nested paths)
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
                } else {
                    return builder.equal(path, castToRequiredType(javaType, rawValue.toString()));
                }
            case NOT_CONTAINS:
                if (javaType == String.class) {
                    return builder.notLike(builder.lower(path.as(String.class)), "%" + stringValue + "%");
                } else {
                    return builder.notEqual(path, castToRequiredType(javaType, rawValue.toString()));
                }
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
            case IN:
                if (rawValue instanceof String && ((String) rawValue).contains(",")) {
                    List<Object> values = Arrays.stream(((String) rawValue).split(","))
                            .map(v -> castToRequiredType(javaType, v.trim()))
                            .collect(Collectors.toList());
                    return path.in(values);
                }
                return path.in(castToRequiredType(javaType, rawValue.toString()));
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
        if (fieldType.isAssignableFrom(Double.class) || fieldType.isAssignableFrom(double.class)) {
            return Double.valueOf(value);
        } else if (fieldType.isAssignableFrom(Integer.class) || fieldType.isAssignableFrom(int.class)) {
            return Integer.valueOf(value);
        } else if (fieldType.isAssignableFrom(Long.class) || fieldType.isAssignableFrom(long.class)) {
            return Long.valueOf(value);
        } else if (fieldType.isAssignableFrom(Boolean.class) || fieldType.isAssignableFrom(boolean.class)) {
            return Boolean.valueOf(value);
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
