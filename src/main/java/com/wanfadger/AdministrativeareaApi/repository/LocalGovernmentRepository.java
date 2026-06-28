package com.wanfadger.AdministrativeareaApi.repository;


import com.wanfadger.AdministrativeareaApi.entity.LocalGovernment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface LocalGovernmentRepository extends JpaRepository<LocalGovernment, String>, JpaSpecificationExecutor<LocalGovernment> {

    @Override
    @EntityGraph(attributePaths = {"subRegion.region"} , type = EntityGraph.EntityGraphType.FETCH)
    List<LocalGovernment> findAll();

    @Override
    @EntityGraph(attributePaths = {"subRegion.region"} , type = EntityGraph.EntityGraphType.FETCH)
    Page<LocalGovernment> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"subRegion.region"} , type = EntityGraph.EntityGraphType.FETCH)
    List<LocalGovernment> findAllBySubRegion_Code(String regionCode);
    @Query("SELECT L FROM LocalGovernment L WHERE L.subRegion.code IN :subRegionCodes")
    List<LocalGovernment> findAllBySubRegionCodes(List<String> subRegionCodes);

    Optional<LocalGovernment> findByNameIgnoreCaseAndSubRegion_Code (String name , String code);



    Optional<LocalGovernment> findByCodeIgnoreCase(String code);




}
