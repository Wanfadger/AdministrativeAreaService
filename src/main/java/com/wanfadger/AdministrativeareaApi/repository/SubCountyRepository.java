package com.wanfadger.AdministrativeareaApi.repository;

import com.wanfadger.AdministrativeareaApi.entity.SubCounty;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubCountyRepository extends AreaRepository<SubCounty> {

    /** Entity graph was MISSING — getOne(SUBCOUNTY) fired one lazy SELECT per ancestor. */
    @Override
    @EntityGraph(attributePaths = {"county.localGovernment.subRegion.region"}, type = EntityGraph.EntityGraphType.FETCH)
    Optional<SubCounty> findByCodeIgnoreCase(String code);

    /**
     * Duplicate check, scoped to the parent county BY CODE.
     *
     * <p>There used to be a sibling {@code findByNameIgnoreCaseAndCounty_Id(String, String)}, and
     * {@code createOne} called it with a {@code partOfCode} — passing a CODE where an ID was
     * expected. Both are Strings, so it compiled; the lookup simply never matched, and duplicate
     * sub-counties were silently created with a 201. (Update used the correct method, which is why
     * only create was affected.)
     *
     * <p>That method is deliberately GONE rather than merely unused: with only the {@code _Code}
     * variant in existence, the mistake cannot be made again.
     */
    Optional<SubCounty> findByNameIgnoreCaseAndCounty_Code(String name, String countyCode);

    /** Child check when deleting a county. */
    boolean existsByCounty_Code(String countyCode);
}
