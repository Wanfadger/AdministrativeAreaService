package com.wanfadger.AdministrativeareaApi.repository;

import com.wanfadger.AdministrativeareaApi.entity.SubRegion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface SubRegionRepository extends JpaRepository<SubRegion, String>, JpaSpecificationExecutor<SubRegion> {

    Optional<SubRegion> findByNameIgnoreCaseAndRegion_Code(String name , String regionCode);

    @EntityGraph(attributePaths = {"region"} , type = EntityGraph.EntityGraphType.FETCH)
    List<SubRegion> findAllByRegion_Code(String code);

    @Override
    @EntityGraph(attributePaths = {"region"} , type = EntityGraph.EntityGraphType.FETCH)
    List<SubRegion> findAll();

    @EntityGraph(attributePaths = {"region"} , type = EntityGraph.EntityGraphType.FETCH)
    Optional<SubRegion> findByCodeIgnoreCase(String code);

}
