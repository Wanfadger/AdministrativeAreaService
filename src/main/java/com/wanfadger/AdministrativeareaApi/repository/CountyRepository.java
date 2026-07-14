package com.wanfadger.AdministrativeareaApi.repository;

import com.wanfadger.AdministrativeareaApi.entity.County;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CountyRepository extends AreaRepository<County> {

    @Override
    @EntityGraph(attributePaths = {"localGovernment.subRegion.region"}, type = EntityGraph.EntityGraphType.FETCH)
    Optional<County> findByCodeIgnoreCase(String code);

    Optional<County> findByNameIgnoreCaseAndLocalGovernment_Code(String name, String localGovernmentCode);

    /** Child check when deleting a local government. */
    boolean existsByLocalGovernment_Code(String localGovernmentCode);
}
