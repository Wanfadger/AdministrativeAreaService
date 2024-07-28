package com.wanfadger.AdministrativeareaApi.repository;


import com.wanfadger.AdministrativeareaApi.entity.LocalGovernment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface LocalGovernmentRepository extends JpaRepository<LocalGovernment, String> {

    @Override
    @EntityGraph(attributePaths = {"subRegion.region"} , type = EntityGraph.EntityGraphType.FETCH)
    List<LocalGovernment> findAll();


    @EntityGraph(attributePaths = {"subRegion.region"} , type = EntityGraph.EntityGraphType.FETCH)
    List<LocalGovernment> findAllBySubRegion_Code(String regionCode);
    @Query("SELECT L FROM LocalGovernment L WHERE L.subRegion.code IN :subRegionCodes")
    List<LocalGovernment> findAllBySubRegionCodes(List<String> subRegionCodes);

    Optional<LocalGovernment> findByNameIgnoreCaseAndSubRegion_Code (String name , String code);



    Optional<LocalGovernment> findByCodeIgnoreCase(String code);




}
