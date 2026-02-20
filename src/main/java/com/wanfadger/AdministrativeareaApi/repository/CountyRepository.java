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

    @org.springframework.data.jpa.repository.Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM County c "
            +
            "JOIN c.localGovernment lg " +
            "JOIN lg.subRegion sr " +
            "JOIN sr.region r " +
            "WHERE LOWER(c.name) = LOWER(:name) " +
            "AND LOWER(lg.name) = LOWER(:lgName) " +
            "AND LOWER(sr.name) = LOWER(:subRegionName) " +
            "AND LOWER(r.name) = LOWER(:regionName)")
    boolean existsByDetails(@org.springframework.data.repository.query.Param("name") String name,
            @org.springframework.data.repository.query.Param("lgName") String lgName,
            @org.springframework.data.repository.query.Param("subRegionName") String subRegionName,
            @org.springframework.data.repository.query.Param("regionName") String regionName);

}
