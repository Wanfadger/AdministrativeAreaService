package com.wanfadger.AdministrativeareaApi.entity;

import lombok.Getter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The six levels, <b>declared parent-first</b>. That ordering is not cosmetic: {@link
 * #selfAndDescendants()} reads the hierarchy straight off the ordinals, so reordering these
 * constants silently changes which caches a write evicts. Add a level in its hierarchical position.
 */
@Getter
public enum AdministrativeAreaType {
    REGION("REGION") ,//REGION
    SUBREGION("SUB REGION"),//SUB-REGION
    LOCALGOVERNMENT("LOCAL GOVERNMENT"), //DISTRICT/LOCAL GOVERNMENT ,
    COUNTY("COUNTY"), //COUNTY/CONSTITUENCY/MUNICIPALITY
    SUBCOUNTY("SUB COUNTY"), //SUB COUNTY/TOWN COUNCIL/DIVISION
    PARISH("PARISH") //PARISH/WARD
    ;

    private final String administrativeAreaType;

    AdministrativeAreaType(String administrativeAreaType) {
        this.administrativeAreaType = administrativeAreaType;
    }

    public static Optional<AdministrativeAreaType> fromStr(String administrativeAreaTypeStr) {
        if (administrativeAreaTypeStr == null) {
            return Optional.empty();
        }
        // Accept both the display value ("SUB REGION") and the enum constant name ("SUBREGION").
        return Arrays.stream(AdministrativeAreaType.values())
                .filter(type -> type.administrativeAreaType.equalsIgnoreCase(administrativeAreaTypeStr)
                        || type.name().equalsIgnoreCase(administrativeAreaTypeStr))
                .findFirst();
    }

    /**
     * This level plus every level beneath it — the set of caches a write to this level invalidates.
     *
     * <p><b>Why downward, and only downward.</b> Every DTO embeds its full <i>ancestry</i>: a
     * {@code ParishDTO} carries its sub-county, county, local government, sub-region and region.
     * So renaming a region genuinely changes the bytes of every cached parish, and the parish caches
     * must go. Nothing embeds its <i>children</i> — a {@code SubCountyDTO} has no parish list — so
     * creating a parish cannot change any cached sub-county, and clearing the sub-county caches
     * would be pure waste.
     *
     * <p>This is the whole reason the cache is split per type. The old code evicted
     * {@code allEntries} on both regions for every write, so renaming one parish discarded every
     * cached page at all six levels: ~7,000 rows of work thrown away to invalidate one row.
     */
    public Set<AdministrativeAreaType> selfAndDescendants() {
        List<AdministrativeAreaType> below =
                Arrays.asList(values()).subList(ordinal(), values().length);
        return EnumSet.copyOf(below);
    }
}
