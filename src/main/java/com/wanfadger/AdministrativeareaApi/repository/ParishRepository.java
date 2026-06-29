package com.wanfadger.AdministrativeareaApi.repository;


import com.wanfadger.AdministrativeareaApi.entity.Parish;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParishRepository extends JpaRepository<Parish, String>, JpaSpecificationExecutor<Parish> {

    @Override
    @EntityGraph(attributePaths = {"subCounty.county.localGovernment.subRegion.region"} , type = EntityGraph.EntityGraphType.FETCH)
    List<Parish> findAll();

    Optional<Parish> findByNameIgnoreCaseAndSubCounty_Code(String name , String countyCode);

    List<Parish> findAllBySubCounty_Code(String subCountyCode);

    Optional<Parish> findByCodeIgnoreCase(String code);

}
