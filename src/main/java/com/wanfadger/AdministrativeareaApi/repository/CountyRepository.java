package com.wanfadger.AdministrativeareaApi.repository;

import com.wanfadger.AdministrativeareaApi.entity.County;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountyRepository extends JpaRepository<County, Long>, JpaSpecificationExecutor<County> {

    @Override
    @EntityGraph(attributePaths = { "localGovernment.subRegion.region" }, type = EntityGraph.EntityGraphType.FETCH)
    @NonNull
    List<County> findAll();

    @Override
    @EntityGraph(attributePaths = { "localGovernment.subRegion.region" }, type = EntityGraph.EntityGraphType.FETCH)
    @NonNull
    Page<County> findAll(@NonNull Pageable pageable);

    @EntityGraph(attributePaths = { "localGovernment", "localGovernment.subRegion",
            "localGovernment.subRegion.region" }, type = EntityGraph.EntityGraphType.FETCH)
    @NonNull
    Optional<County> findByCodeIgnoreCase(@NonNull String code);

    List<County> findByNameIgnoreCaseIn(List<String> names);

    Optional<County> findByNameIgnoreCaseAndLocalGovernment_Code(String name, String localGovernmentCode);

    Optional<County> findByCodeIgnoreCaseAndLocalGovernment_Code(String code, String localGovernmentCode);

    boolean existsByNameIgnoreCaseAndLocalGovernment_Code(String name, String code);

    boolean existsByLocalGovernment_Code(String localGovernmentCode);

    List<County> findByCodeIgnoreCaseIn(List<String> codes);

    boolean existsByNameIgnoreCaseAndLocalGovernment_NameIgnoreCaseAndLocalGovernment_SubRegion_NameIgnoreCaseAndSubRegion_Region_NameIgnoreCase(
            String name, String localGovernmentName, String subRegionName, String regionName);

}
