package com.wanfadger.AdministrativeareaApi.repository.specification;

/**
 * Filter operators supported by the {@code field[:operator]=value} search convention.
 * Mirrors the URRMS Core {@code MatchType} so search ergonomics stay identical across services.
 */
public enum MatchType {
    EQUALS,
    NOT_EQUALS,
    CONTAINS,
    NOT_CONTAINS,
    GT,
    LT,
    GTE,
    LTE,
    IN
}
