package com.wanfadger.AdministrativeareaApi.service.query;

/** Shape of the items in a search response. */
public enum View {

    /**
     * Every item carries its full parent chain (a parish embeds subCounty → county →
     * localGovernment → subRegion → region). The default, and the shape the Angular console
     * depends on.
     */
    NESTED,

    /**
     * Every item carries only {@code partOfCode} — its immediate parent's code — instead of the
     * nested ancestry. Roughly a tenth of the payload, and the SQL drops from a six-table join to
     * two. Opt-in via {@code ?view=flat}, for integrating services that only need identity.
     */
    FLAT;

    /**
     * Anything that is not exactly "flat" (absent, blank, "nested", or junk) resolves to NESTED.
     * Normalising here rather than passing the raw string through is what stops {@code view=},
     * {@code view=NESTED} and {@code view=garbage} minting three separate cache entries for one
     * identical response.
     */
    public static View from(String raw) {
        return raw != null && raw.equalsIgnoreCase("flat") ? FLAT : NESTED;
    }
}
