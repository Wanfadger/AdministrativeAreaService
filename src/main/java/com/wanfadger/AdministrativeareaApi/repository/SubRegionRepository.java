package com.wanfadger.AdministrativeareaApi.repository;

import com.wanfadger.AdministrativeareaApi.entity.SubRegion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface SubRegionRepository extends JpaRepository<SubRegion, String> {

    Optional<SubRegion> findByNameIgnoreCase(String name);



    Optional<SubRegion> findByNameIgnoreCaseAndRegion_Code(String name , String regionCod);

    @EntityGraph(attributePaths = {"region"} , type = EntityGraph.EntityGraphType.FETCH)
    List<SubRegion> findAllByRegion_Code(String code);



    @Override
    @EntityGraph(attributePaths = {"region"} , type = EntityGraph.EntityGraphType.FETCH)
    List<SubRegion> findAll();

    @EntityGraph(attributePaths = {"region"} , type = EntityGraph.EntityGraphType.FETCH)
    Optional<SubRegion> findByCodeIgnoreCase(String code);



}
