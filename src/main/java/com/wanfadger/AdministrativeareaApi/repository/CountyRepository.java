package com.wanfadger.AdministrativeareaApi.repository;

import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.entity.County;
import com.wanfadger.AdministrativeareaApi.repository.projections.CodeNameProjection;
import lombok.NonNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface CountyRepository extends JpaRepository<County, String> {

    @Override
    @EntityGraph(attributePaths = {"localGovernment.subRegion.region"} , type = EntityGraph.EntityGraphType.FETCH)
    @NonNull
    List<County> findAll();


    @EntityGraph(attributePaths = {"localGovernment.subRegion.region"} , type = EntityGraph.EntityGraphType.FETCH)
    @NonNull
    Optional<County> findByCodeIgnoreCase(@NonNull String code);

    List<County> findAllByCodeIgnoreCase(@NonNull String code);

    Optional<County> findByNameIgnoreCaseAndLocalGovernment_Code(String name , String localGovernmentCode);

    @EntityGraph(attributePaths = {"localGovernment.subRegion.region"} , type = EntityGraph.EntityGraphType.FETCH)
    List<County> findAllByLocalGovernment_Code(String localGovernmentCode);

    @Query("SELECT C FROM County C WHERE C.localGovernment.code IN :codes")
    List<County> findAllByLocalGovernmentCodes(List<String> codes);

    @Query("SELECT C.code as code , C.name as name FROM County C WHERE upper(C.localGovernment.code) = upper(:localGovernment) ")
    List<CodeNameProjection> dbCodeNameList(String localGovernment);


    @Query("SELECT C.code as code , C.name as name FROM County C WHERE upper(C.code) = upper(:code) ")
    Optional<CodeNameProjection> dbCodeName(String code);


}
