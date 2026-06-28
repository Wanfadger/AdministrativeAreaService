package com.wanfadger.AdministrativeareaApi.repository;

import com.wanfadger.AdministrativeareaApi.entity.SubRegion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface SubRegionRepository extends JpaRepository<SubRegion, String>, JpaSpecificationExecutor<SubRegion> {

    Optional<SubRegion> findByNameIgnoreCase(String name);



    Optional<SubRegion> findByNameIgnoreCaseAndRegion_Code(String name , String regionCode);

    @EntityGraph(attributePaths = {"region"} , type = EntityGraph.EntityGraphType.FETCH)
    List<SubRegion> findAllByRegion_Code(String code);



    @Override
    @EntityGraph(attributePaths = {"region"} , type = EntityGraph.EntityGraphType.FETCH)
    List<SubRegion> findAll();

    @Override
    @EntityGraph(attributePaths = {"region"} , type = EntityGraph.EntityGraphType.FETCH)
    Page<SubRegion> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"region"} , type = EntityGraph.EntityGraphType.FETCH)
    Optional<SubRegion> findByCodeIgnoreCase(String code);



}
