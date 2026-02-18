package com.wanfadger.AdministrativeareaApi.repository;

import com.wanfadger.AdministrativeareaApi.entity.Parish;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParishRepository extends JpaRepository<Parish, Long> {

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

        List<Parish> findAllBySubCounty_Code(String subCountyCode);

        @Query("SELECT P FROM Parish P WHERE P.subCounty.code IN :subCountyCodes")
        List<Parish> findAllBySubCountyCodes(List<String> subCountyCodes);

        Optional<Parish> findByCodeIgnoreCase(String code);

}
