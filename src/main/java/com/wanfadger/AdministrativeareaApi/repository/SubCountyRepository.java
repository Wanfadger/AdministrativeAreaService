package com.wanfadger.AdministrativeareaApi.repository;


import com.wanfadger.AdministrativeareaApi.entity.SubCounty;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubCountyRepository extends JpaRepository<SubCounty, String> {

    @Override
    @EntityGraph(attributePaths = {"county.localGovernment.subRegion.region"} , type = EntityGraph.EntityGraphType.FETCH)
    List<SubCounty> findAll();

    @Override
    @EntityGraph(attributePaths = {"county.localGovernment.subRegion.region"} , type = EntityGraph.EntityGraphType.FETCH)
    Optional<SubCounty> findById(String id);

    Optional<SubCounty> findByNameIgnoreCaseAndCounty_Id(String name , String countyId);

    @EntityGraph(attributePaths = {"county.localGovernment.subRegion.region"} , type = EntityGraph.EntityGraphType.FETCH)
    List<SubCounty> findAllByCounty_Code(String countyCode);

    @Query("SELECT SC FROM SubCounty SC WHERE SC.county.code IN :countyCodes")
    List<SubCounty> findAllByCountyCodes(List<String> countyCodes);

    Optional<SubCounty> findByCodeIgnoreCase(String code);



}
