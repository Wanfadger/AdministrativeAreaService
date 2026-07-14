package com.wanfadger.AdministrativeareaApi.repository;

import com.wanfadger.AdministrativeareaApi.entity.Parish;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParishRepository extends AreaRepository<Parish> {

    /**
     * Entity graph was MISSING, and this is the worst case of the three: a parish has five
     * ancestors, so getOne(PARISH) mapped the full chain via 1 + 5 = six queries. Now one.
     */
    @Override
    @EntityGraph(attributePaths = {"subCounty.county.localGovernment.subRegion.region"},
                 type = EntityGraph.EntityGraphType.FETCH)
    Optional<Parish> findByCodeIgnoreCase(String code);

    Optional<Parish> findByNameIgnoreCaseAndSubCounty_Code(String name, String subCountyCode);

    /** Child check when deleting a sub-county. Parish is the leaf, so it has no check of its own. */
    boolean existsBySubCounty_Code(String subCountyCode);
}
