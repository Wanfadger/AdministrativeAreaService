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

        Optional<Parish> findByNameIgnoreCaseAndSubCounty_Code(String name, String countyCode);

        @EntityGraph(attributePaths = { "subCounty", "subCounty.county", "subCounty.county.localGovernment",
                        "subCounty.county.localGovernment.subRegion",
                        "subCounty.county.localGovernment.subRegion.region" }, type = EntityGraph.EntityGraphType.FETCH)
        Optional<Parish> findByCodeIgnoreCase(String code);

        List<Parish> findByCodeIgnoreCaseIn(List<String> codes);

        List<Parish> findByNameIgnoreCaseIn(List<String> names);

        Optional<Parish> findByCodeIgnoreCaseAndSubCounty_Code(String code, String subCountyCode);

        boolean existsByNameIgnoreCaseAndSubCounty_Code(String name, String code);

        boolean existsBySubCounty_Code(String subCountyCode);

        boolean existsByNameIgnoreCaseAndSubCounty_NameIgnoreCaseAndSubCounty_County_NameIgnoreCaseAndSubCounty_County_LocalGovernment_NameIgnoreCaseAndSubCounty_County_LocalGovernment_SubRegion_NameIgnoreCaseAndSubCounty_County_LocalGovernment_SubRegion_Region_NameIgnoreCase(
                        String name, String subCountyName, String countyName, String localGovernmentName,
                        String subRegionName, String regionName);

}
