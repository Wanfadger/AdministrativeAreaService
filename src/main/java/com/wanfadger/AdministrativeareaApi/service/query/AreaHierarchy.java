package com.wanfadger.AdministrativeareaApi.service.query;

import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The chain of {@code @ManyToOne} property names that links each level to the one above it.
 *
 * <p>{@link AdministrativeAreaType} already encodes the hierarchy in its ordinals; this adds the
 * one thing the ordinals cannot say — what the association is <em>called</em> on the entity — so a
 * JPA property path can be built from any level up to any ancestor.
 *
 * <p><b>Why this needs to exist.</b> {@code partOf} filters on the immediate parent only, so
 * answering "every parish in this region" from outside meant walking five levels one request at a
 * time. Because {@code partOf} also takes a single code, that walk fans out per parent: measured
 * against the live gazetteer, 609 requests for the largest region — past the 300/minute rate limit,
 * for one question. The join those requests are reconstructing already exists in the schema, is
 * already traversed for the nested view, and {@code GenericSpecification} already turns a dotted
 * path into it. All that was missing was permission to ask.
 *
 * <p>So this class no longer answers the question itself; it says which paths are legal, and the
 * ordinary {@code field:OPERATOR} filter does the rest. That replaced a dedicated
 * {@code ancestorType}/{@code ancestorCode} pair, which worked but could only ever match an
 * ancestor's code — "every county in the region called Northern" needed a second feature, and the
 * one after that would have needed a third.
 *
 * <p>Kept as a lookup rather than derived from the entity metamodel because the names are contract:
 * they appear in {@code fetchChain(...)} and in every handler's {@code parentCodePath()}, and a
 * rename there has to be a deliberate, visible edit here too.
 */
public final class AreaHierarchy {

    /** Each level's association to its parent. REGION is absent — it is the root. */
    private static final Map<AdministrativeAreaType, String> PARENT_PROPERTY =
            new EnumMap<>(AdministrativeAreaType.class);

    static {
        PARENT_PROPERTY.put(AdministrativeAreaType.SUBREGION, "region");
        PARENT_PROPERTY.put(AdministrativeAreaType.LOCALGOVERNMENT, "subRegion");
        PARENT_PROPERTY.put(AdministrativeAreaType.COUNTY, "localGovernment");
        PARENT_PROPERTY.put(AdministrativeAreaType.SUBCOUNTY, "county");
        PARENT_PROPERTY.put(AdministrativeAreaType.PARISH, "subCounty");
    }

    private AreaHierarchy() {
    }

    /** Prefixes per level, built once — the hierarchy is fixed at class-load. */
    private static final Map<AdministrativeAreaType, Map<AdministrativeAreaType, String>> PREFIXES =
            new EnumMap<>(AdministrativeAreaType.class);

    static {
        for (AdministrativeAreaType type : AdministrativeAreaType.values()) {
            PREFIXES.put(type, Collections.unmodifiableMap(buildPrefixes(type)));
        }
    }

    /**
     * Every ancestor of {@code type}, nearest first, with the JPA property path prefix that reaches
     * it — so a PARISH search maps REGION to
     * {@code subCounty.county.localGovernment.subRegion.region.} and any filterable column appended
     * to that is a legal filter.
     *
     * <p>This is what makes one filter syntax cover the whole hierarchy. A caller wanting every
     * county in a region by that region's <em>name</em> sends
     * {@code type=COUNTY&localGovernment.subRegion.region.name:EQUALS=Northern} — the same
     * {@code field:OPERATOR} form as any other filter, with every operator available, over any
     * column rather than just the code. The alternative was a dedicated pair of parameters that
     * could only ever match on code, and needed extending for each new thing anyone wanted.
     *
     * <p>Bounded on purpose. Allowing arbitrary dotted paths would let a caller ask for join chains
     * this schema does not have, and would put unbounded cardinality back into the cache key that
     * {@code AreaQueryFactory} exists to keep bounded. Six levels give at most five prefixes, so
     * the legal set per level is small, enumerable, and can be printed in an error message.
     *
     * <p>Empty for REGION, which has no ancestors.
     */
    public static Map<AdministrativeAreaType, String> ancestorPathPrefixes(AdministrativeAreaType type) {
        return type == null ? Map.of() : PREFIXES.getOrDefault(type, Map.of());
    }

    private static Map<AdministrativeAreaType, String> buildPrefixes(AdministrativeAreaType type) {
        Map<AdministrativeAreaType, String> prefixes = new LinkedHashMap<>();
        StringBuilder path = new StringBuilder();
        AdministrativeAreaType current = type;
        while (current.ordinal() > 0) {
            String property = PARENT_PROPERTY.get(current);
            if (property == null) {
                break;   // unreachable while the map is complete
            }
            path.append(property).append('.');
            current = AdministrativeAreaType.values()[current.ordinal() - 1];
            prefixes.put(current, path.toString());
        }
        return prefixes;
    }
}
