package com.wanfadger.AdministrativeareaApi.repository;

import com.wanfadger.AdministrativeareaApi.entity.SubCounty;
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
public interface SubCountyRepository extends JpaRepository<SubCounty, Long>, JpaSpecificationExecutor<SubCounty> {

        @Override
        @EntityGraph(attributePaths = {
                        "county.localGovernment.subRegion.region" }, type = EntityGraph.EntityGraphType.FETCH)
        @NonNull
        List<SubCounty> findAll();

        @Override
        @EntityGraph(attributePaths = {
                        "county.localGovernment.subRegion.region" }, type = EntityGraph.EntityGraphType.FETCH)
        @NonNull
        Page<SubCounty> findAll(@NonNull Pageable pageable);

        @Override
        @EntityGraph(attributePaths = {
                        "county.localGovernment.subRegion.region" }, type = EntityGraph.EntityGraphType.FETCH)
        @NonNull
        Optional<SubCounty> findById(@NonNull Long id);

        Optional<SubCounty> findByNameIgnoreCaseAndCounty_Id(String name, Long countyId);

        Optional<SubCounty> findByCodeIgnoreCase(String code);

        Optional<SubCounty> findByCodeIgnoreCaseAndCounty_Code(String code, String countyCode);

        boolean existsByNameIgnoreCaseAndCounty_Code(String name, String code);

        boolean existsByCounty_Code(String countyCode);

        List<SubCounty> findByCodeIgnoreCaseIn(List<String> codes);

        List<SubCounty> findByNameIgnoreCaseIn(List<String> names);

        @org.springframework.data.jpa.repository.Query("SELECT CASE WHEN COUNT(sc) > 0 THEN true ELSE false END FROM SubCounty sc "
                        +
                        "JOIN sc.county c " +
                        "JOIN c.localGovernment lg " +
                        "JOIN lg.subRegion sr " +
                        "JOIN sr.region r " +
                        "WHERE LOWER(sc.name) = LOWER(:name) " +
                        "AND LOWER(c.name) = LOWER(:countyName) " +
                        "AND LOWER(lg.name) = LOWER(:lgName) " +
                        "AND LOWER(sr.name) = LOWER(:subRegionName) " +
                        "AND LOWER(r.name) = LOWER(:regionName)")
        boolean existsByDetails(@org.springframework.data.repository.query.Param("name") String name,
                        @org.springframework.data.repository.query.Param("countyName") String countyName,
                        @org.springframework.data.repository.query.Param("lgName") String lgName,
                        @org.springframework.data.repository.query.Param("subRegionName") String subRegionName,
                        @org.springframework.data.repository.query.Param("regionName") String regionName);

}
