package com.wanfadger.AdministrativeareaApi.repository;

import com.wanfadger.AdministrativeareaApi.entity.Parish;
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
public interface ParishRepository extends JpaRepository<Parish, Long>, JpaSpecificationExecutor<Parish> {

        @Override
        @EntityGraph(attributePaths = {
                        "subCounty.county.localGovernment.subRegion.region" }, type = EntityGraph.EntityGraphType.FETCH)
        @NonNull
        List<Parish> findAll();

        boolean existsByCodeIgnoreCase(String code);

        @Override
        @EntityGraph(attributePaths = {
                        "subCounty.county.localGovernment.subRegion.region" }, type = EntityGraph.EntityGraphType.FETCH)
        @NonNull
        Page<Parish> findAll(@NonNull Pageable pageable);

        @Override
        @EntityGraph(attributePaths = {
                        "subCounty.county.localGovernment.subRegion.region" }, type = EntityGraph.EntityGraphType.FETCH)
        @NonNull
        Optional<Parish> findById(@NonNull Long id);

        @EntityGraph(attributePaths = { "subCounty", "subCounty.county", "subCounty.county.localGovernment",
                        "subCounty.county.localGovernment.subRegion",
                        "subCounty.county.localGovernment.subRegion.region" }, type = EntityGraph.EntityGraphType.FETCH)
        Optional<Parish> findByCodeIgnoreCase(String code);

        List<Parish> findByCodeIgnoreCaseIn(List<String> codes);

        List<Parish> findByNameIgnoreCaseIn(List<String> names);

        Optional<Parish> findByCodeIgnoreCaseAndSubCounty_Code(String code, String subCountyCode);

        boolean existsByNameIgnoreCaseAndSubCounty_Code(String name, String code);

        boolean existsBySubCounty_Code(String subCountyCode);

        @org.springframework.data.jpa.repository.Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Parish p "
                        +
                        "JOIN p.subCounty sc " +
                        "JOIN sc.county c " +
                        "JOIN c.localGovernment lg " +
                        "JOIN lg.subRegion sr " +
                        "JOIN sr.region r " +
                        "WHERE LOWER(p.name) = LOWER(:name) " +
                        "AND LOWER(sc.name) = LOWER(:subCountyName) " +
                        "AND LOWER(c.name) = LOWER(:countyName) " +
                        "AND LOWER(lg.name) = LOWER(:lgName) " +
                        "AND LOWER(sr.name) = LOWER(:subRegionName) " +
                        "AND LOWER(r.name) = LOWER(:regionName)")
        boolean existsByDetails(@org.springframework.data.repository.query.Param("name") String name,
                        @org.springframework.data.repository.query.Param("subCountyName") String subCountyName,
                        @org.springframework.data.repository.query.Param("countyName") String countyName,
                        @org.springframework.data.repository.query.Param("lgName") String lgName,
                        @org.springframework.data.repository.query.Param("subRegionName") String subRegionName,
                        @org.springframework.data.repository.query.Param("regionName") String regionName);

}
