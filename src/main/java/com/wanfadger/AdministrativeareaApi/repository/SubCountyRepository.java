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

        boolean existsByNameIgnoreCaseAndCounty_Code(String name, String code);

        List<SubCounty> findByCodeIgnoreCaseIn(List<String> codes);

}
