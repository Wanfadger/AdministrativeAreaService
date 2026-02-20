package com.wanfadger.AdministrativeareaApi.repository;

import com.wanfadger.AdministrativeareaApi.entity.SubRegion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

        Optional<SubRegion> findByCodeIgnoreCase(String code);

        List<SubRegion> findByCodeIgnoreCaseIn(List<String> codes);

        List<SubRegion> findByNameIgnoreCaseIn(List<String> names);

        @Query("SELECT CASE WHEN COUNT(sr) > 0 THEN true ELSE false END FROM SubRegion sr "
                        + "JOIN sr.region r "
                        + "WHERE LOWER(sr.name) = LOWER(:name) "
                        + "AND LOWER(r.name) = LOWER(:regionName)")
        boolean existsByDetails(@Param("name") String name,
                        @Param("regionName") String regionName);

}
