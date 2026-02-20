package com.wanfadger.AdministrativeareaApi.repository;

import com.wanfadger.AdministrativeareaApi.entity.County;

import io.lettuce.core.dynamic.annotation.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountyRepository extends JpaRepository<County, Long>, JpaSpecificationExecutor<County> {

        @NonNull
        Optional<County> findByCodeIgnoreCase(@NonNull String code);

        List<County> findByNameIgnoreCaseIn(List<String> names);

        Optional<County> findByCodeIgnoreCaseAndLocalGovernment_Code(String code, String localGovernmentCode);

        boolean existsByNameIgnoreCaseAndLocalGovernment_Code(String name, String code);

        boolean existsByLocalGovernment_Code(String localGovernmentCode);

        boolean existsByCodeIgnoreCase(String code);

        List<County> findByCodeIgnoreCaseIn(List<String> codes);

        @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM County c "
                        +
                        "JOIN c.localGovernment lg " +
                        "JOIN lg.subRegion sr " +
                        "JOIN sr.region r " +
                        "WHERE LOWER(c.name) = LOWER(:name) " +
                        "AND LOWER(lg.name) = LOWER(:lgName) " +
                        "AND LOWER(sr.name) = LOWER(:subRegionName) " +
                        "AND LOWER(r.name) = LOWER(:regionName)")
        boolean existsByDetails(@Param("name") String name,
                        @Param("lgName") String lgName,
                        @Param("subRegionName") String subRegionName,
                        @Param("regionName") String regionName);

}
