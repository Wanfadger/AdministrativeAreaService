package com.wanfadger.AdministrativeareaApi.repository;

import com.wanfadger.AdministrativeareaApi.entity.SubCounty;

import io.lettuce.core.dynamic.annotation.Param;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubCountyRepository extends JpaRepository<SubCounty, Long>, JpaSpecificationExecutor<SubCounty> {
        boolean existsByCodeIgnoreCase(String code);

        Optional<SubCounty> findByCodeIgnoreCase(String code);

        Optional<SubCounty> findByCodeIgnoreCaseAndCounty_Code(String code, String countyCode);

        boolean existsByNameIgnoreCaseAndCounty_Code(String name, String code);

        boolean existsByCounty_Code(String countyCode);

        List<SubCounty> findByCodeIgnoreCaseIn(List<String> codes);

        List<SubCounty> findByNameIgnoreCaseIn(List<String> names);

        @Query("SELECT CASE WHEN COUNT(sc) > 0 THEN true ELSE false END FROM SubCounty sc "
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
        boolean existsByDetails(@Param("name") String name,
                        @Param("countyName") String countyName,
                        @Param("lgName") String lgName,
                        @Param("subRegionName") String subRegionName,
                        @Param("regionName") String regionName);

}
