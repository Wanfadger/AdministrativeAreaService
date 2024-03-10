package com.wanfadger.AdministrativeareaApi.repository;

import com.wanfadger.AdministrativeareaApi.entity.AdministrativeArea;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Deprecated
public interface AdministrativeAreaRepository extends JpaRepository<AdministrativeArea , String> {
    // uique by name , type ,partOf

//    Optional<AdministrativeArea> findByNameIgnoreCaseAndAdministrativeAreaTypeAndPartOf(String name , AdministrativeAreaType administrativeAreaType , String partOf);
//
//    List<AdministrativeArea> findByPartOf(String partOf);
//    List<AdministrativeArea> findByAdministrativeAreaType(AdministrativeAreaType administrativeAreaType);
//
//    Optional<AdministrativeArea> findByCodeIgnoreCase(String code);
//    Optional<AdministrativeArea> findByNameIgnoreCase(String name);
//
//    List<AdministrativeArea> findAllByNameLikeIgnoreCase(String name);
//    List<AdministrativeArea> findAllByCodeLikeIgnoreCase(String code);

}
