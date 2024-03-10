package com.wanfadger.AdministrativeareaApi.repository;

import com.wanfadger.AdministrativeareaApi.entity.SubRegion;
import com.wanfadger.AdministrativeareaApi.repository.projections.CodeNameProjection;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface SubRegionRepository extends JpaRepository<SubRegion, String> {

    Optional<SubRegion> findByNameIgnoreCase(String name);

    @Query("SELECT SR.code as code , SR.name as name FROM SubRegion SR  WHERE upper(SR.region.code) = upper(:region) ")
    List<CodeNameProjection> dbCodeNameList(String region);

    @Query("SELECT SR.code as code , SR.name as name FROM SubRegion SR  WHERE upper(SR.code) = upper(:code) ")
    Optional<CodeNameProjection> dbCodeName(String code);

    Optional<SubRegion> findByNameIgnoreCaseAndRegion_Code(String name , String regionCod);

    @EntityGraph(attributePaths = {"region"})
    List<SubRegion> findAllByRegion_Code(String code);

    @Override
    @EntityGraph(attributePaths = {"region"})
    List<SubRegion> findAll();

    @EntityGraph(attributePaths = {"region"} , type = EntityGraph.EntityGraphType.FETCH)
    Optional<SubRegion> findByCodeIgnoreCase(String code);


}
