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

    @EntityGraph(attributePaths = { "localGovernment.subRegion.region" }, type = EntityGraph.EntityGraphType.FETCH)
    @NonNull
    Optional<County> findByCodeIgnoreCase(@NonNull String code);

    Optional<County> findByNameIgnoreCaseAndLocalGovernment_Code(String name, String localGovernmentCode);

    Optional<County> findByCodeIgnoreCaseAndLocalGovernment_Code(String code, String localGovernmentCode);

    boolean existsByNameIgnoreCaseAndLocalGovernment_Code(String name, String code);

    List<County> findByCodeIgnoreCaseIn(List<String> codes);

}
