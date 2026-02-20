package com.wanfadger.AdministrativeareaApi.repository;

import com.wanfadger.AdministrativeareaApi.entity.SubRegion;
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
public interface SubRegionRepository extends JpaRepository<SubRegion, Long>, JpaSpecificationExecutor<SubRegion> {

        Optional<SubRegion> findByNameIgnoreCase(String name);

        Optional<SubRegion> findByCodeIgnoreCaseAndRegion_Code(String code, String regionCode);

        boolean existsByNameIgnoreCaseAndRegion_Code(String name, String regionCode);

        boolean existsByNameIgnoreCaseAndRegion_NameIgnoreCase(String subRegionName, String regionName);

        boolean existsByRegion_Code(String regionCode);

        boolean existsByCodeIgnoreCase(String code);

        @Override
        @EntityGraph(attributePaths = { "region" }, type = EntityGraph.EntityGraphType.FETCH)
        @NonNull
        List<SubRegion> findAll();

        @Override
        @EntityGraph(attributePaths = { "region" }, type = EntityGraph.EntityGraphType.FETCH)
        @NonNull
        Page<SubRegion> findAll(@NonNull Pageable pageable);

        @EntityGraph(attributePaths = { "region" }, type = EntityGraph.EntityGraphType.FETCH)
        Optional<SubRegion> findByCodeIgnoreCase(String code);

        List<SubRegion> findByCodeIgnoreCaseIn(List<String> codes);

        List<SubRegion> findByNameIgnoreCaseIn(List<String> names);

        @org.springframework.data.jpa.repository.Query("SELECT CASE WHEN COUNT(sr) > 0 THEN true ELSE false END FROM SubRegion sr "
                        + "JOIN sr.region r "
                        + "WHERE LOWER(sr.name) = LOWER(:name) "
                        + "AND LOWER(r.name) = LOWER(:regionName)")
        boolean existsByDetails(@org.springframework.data.repository.query.Param("name") String name,
                        @org.springframework.data.repository.query.Param("regionName") String regionName);

}
