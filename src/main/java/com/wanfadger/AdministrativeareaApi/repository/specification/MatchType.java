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
    IN,
    /**
     * Field is NULL. The supplied value is ignored — {@code field:IS_NULL=true} and
     * {@code field:IS_NULL=anything} mean the same thing; a value has to be sent only because
     * blank-valued filters are skipped before they reach the specification.
     *
     * <p>Added for "unassigned only" listings, which cannot otherwise be expressed: every other
     * operator compares against a value, and NULL is not equal to anything, itself included.
     */
    IS_NULL,
    /** Field is NOT NULL. The supplied value is ignored, as with {@link #IS_NULL}. */
    IS_NOT_NULL
}
