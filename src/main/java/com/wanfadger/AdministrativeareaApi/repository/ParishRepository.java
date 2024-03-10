package com.wanfadger.AdministrativeareaApi.repository;


import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.entity.Parish;
import com.wanfadger.AdministrativeareaApi.repository.projections.CodeNameProjection;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParishRepository extends JpaRepository<Parish, String> {

    @Override
    @EntityGraph(attributePaths = {"subCounty.county.localGovernment.subRegion.region"} , type = EntityGraph.EntityGraphType.FETCH)
    List<Parish> findAll();

    @Override
    @EntityGraph(attributePaths = {"subCounty.county.localGovernment.subRegion.region"} , type = EntityGraph.EntityGraphType.FETCH)
    Optional<Parish> findById(String id);

    Optional<Parish> findByNameIgnoreCaseAndSubCounty_Code(String name , String countyCode);

    List<Parish> findAllBySubCounty_Code(String subCountyCode);

    Optional<Parish> findByCodeIgnoreCase(String code);

    @Query("SELECT P.code as code , P.name as name FROM Parish P WHERE upper(P.subCounty.code) = upper(:code) ")
    List<CodeNameProjection> dbIdNameList(String code);

    @Query("SELECT P.code as code , P.name as name FROM Parish P WHERE upper(P.code) = upper(:code) ")
    Optional<CodeNameProjection> dbCodeName(String code);

}
