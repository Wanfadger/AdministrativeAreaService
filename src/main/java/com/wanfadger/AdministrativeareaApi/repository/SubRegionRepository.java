package com.wanfadger.AdministrativeareaApi.repository;

import com.wanfadger.AdministrativeareaApi.entity.SubRegion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubRegionRepository extends AreaRepository<SubRegion> {

    @Override
    @EntityGraph(attributePaths = {"region"}, type = EntityGraph.EntityGraphType.FETCH)
    Optional<SubRegion> findByCodeIgnoreCase(String code);

    /** Scoped to the parent region — matching the parent-scoped unique index added in V3. */
    Optional<SubRegion> findByNameIgnoreCaseAndRegion_Code(String name, String regionCode);

    /**
     * Child check when deleting a region. Renders as {@code SELECT 1 ... LIMIT 1} rather than
     * hydrating every sub-region (each dragging its entity-graph-fetched region along) just to call
     * {@code isEmpty()}. {@code @SQLRestriction("archived = false")} still applies, so archived
     * children correctly do not block the delete — identical semantics, a fraction of the work.
     */
    boolean existsByRegion_Code(String regionCode);
}
