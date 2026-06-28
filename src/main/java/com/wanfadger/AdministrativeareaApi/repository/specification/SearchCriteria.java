package com.wanfadger.AdministrativeareaApi.repository.specification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single search predicate triple (key, value, matchType) consumed by
 * {@link GenericSpecification}.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchCriteria {
    private String key;
    private Object value;
    private MatchType matchType;
}
