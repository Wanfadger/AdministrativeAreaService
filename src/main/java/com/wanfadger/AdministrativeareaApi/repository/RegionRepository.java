package com.wanfadger.AdministrativeareaApi.repository;

import com.wanfadger.AdministrativeareaApi.entity.Region;
import com.wanfadger.AdministrativeareaApi.repository.projections.CodeNameProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface RegionRepository extends JpaRepository<Region, String> {

    Optional<Region> findByNameIgnoreCase(String name);
    Optional<Region> findByCodeIgnoreCase(String code);

    @Query("SELECT R.code as code , R.name as name FROM Region R ")
    List<CodeNameProjection> dbCodeNameList();

    @Query("SELECT R.code as code , R.name as name FROM Region R WHERE UPPER(R.code) = UPPER(:code) ")
    Optional<CodeNameProjection> dbCodeName(String code);


//    List<AdministrativeArea> findByPartOf(String partOf);
//    List<AdministrativeArea> findByAdministrativeAreaType(AdministrativeAreaType administrativeAreaType);
//

//    List<AdministrativeArea> findAllByNameLikeIgnoreCase(String name);
//    List<AdministrativeArea> findAllByCodeLikeIgnoreCase(String code);
}
