package com.wanfadger.AdministrativeareaApi.repository;

import com.wanfadger.AdministrativeareaApi.entity.LocalGovernment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocalGovernmentRepository
                extends JpaRepository<LocalGovernment, Long>, JpaSpecificationExecutor<LocalGovernment> {

        boolean existsByCodeIgnoreCase(String code);

        Optional<LocalGovernment> findByCodeIgnoreCase(String code);

        Optional<LocalGovernment> findByCodeIgnoreCaseAndSubRegion_Code(String code, String parentCode);

        boolean existsByNameIgnoreCaseAndSubRegion_Code(String name, String code);

        boolean existsBySubRegion_Code(String subRegionCode);

        List<LocalGovernment> findByCodeIgnoreCaseIn(List<String> codes);

        List<LocalGovernment> findByNameIgnoreCaseIn(List<String> names);

        boolean existsByNameIgnoreCaseAndSubRegion_NameIgnoreCase(String name, String subRegionName);

        @Query("SELECT CASE WHEN COUNT(lg) > 0 THEN true ELSE false END FROM LocalGovernment lg "
                        + "JOIN lg.subRegion sr "
                        + "JOIN sr.region r "
                        + "WHERE LOWER(lg.name) = LOWER(:name) "
                        + "AND LOWER(sr.name) = LOWER(:subRegionName) "
                        + "AND LOWER(r.name) = LOWER(:regionName)")
        boolean existsByDetails(@org.springframework.data.repository.query.Param("name") String name,
                        @org.springframework.data.repository.query.Param("subRegionName") String subRegionName,
                        @org.springframework.data.repository.query.Param("regionName") String regionName);

}
