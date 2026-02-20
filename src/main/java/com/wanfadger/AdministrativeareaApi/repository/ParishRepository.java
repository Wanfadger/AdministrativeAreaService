package com.wanfadger.AdministrativeareaApi.repository;

import com.wanfadger.AdministrativeareaApi.entity.Parish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParishRepository extends JpaRepository<Parish, Long>, JpaSpecificationExecutor<Parish> {
        boolean existsByCodeIgnoreCase(String code);

        Optional<Parish> findByCodeIgnoreCase(String code);

        List<Parish> findByCodeIgnoreCaseIn(List<String> codes);

        List<Parish> findByNameIgnoreCaseIn(List<String> names);

        Optional<Parish> findByCodeIgnoreCaseAndSubCounty_Code(String code, String subCountyCode);

        boolean existsByNameIgnoreCaseAndSubCounty_Code(String name, String code);

        boolean existsBySubCounty_Code(String subCountyCode);

        @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Parish p "
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
        boolean existsByDetails(@Param("name") String name,
                        @Param("subCountyName") String subCountyName,
                        @Param("countyName") String countyName,
                        @Param("lgName") String lgName,
                        @Param("subRegionName") String subRegionName,
                        @Param("regionName") String regionName);

}
