package com.wanfadger.AdministrativeareaApi.repository;

import com.wanfadger.AdministrativeareaApi.entity.LocalGovernment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LocalGovernmentRepository extends AreaRepository<LocalGovernment> {

    /**
     * The entity graph was MISSING here. getOne(LOCALGOVERNMENT) maps the full ancestor chain, so
     * without it each ancestor was a separate lazy SELECT.
     */
    @Override
    @EntityGraph(attributePaths = {"subRegion.region"}, type = EntityGraph.EntityGraphType.FETCH)
    Optional<LocalGovernment> findByCodeIgnoreCase(String code);

    /** Scoped to the parent sub-region — matching the parent-scoped unique index added in V3. */
    Optional<LocalGovernment> findByNameIgnoreCaseAndSubRegion_Code(String name, String subRegionCode);

    /** Child check when deleting a sub-region. See SubRegionRepository#existsByRegion_Code. */
    boolean existsBySubRegion_Code(String subRegionCode);
}
