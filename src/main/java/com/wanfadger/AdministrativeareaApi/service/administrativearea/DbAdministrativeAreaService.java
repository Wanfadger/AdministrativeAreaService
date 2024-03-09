package com.wanfadger.AdministrativeareaApi.service.administrativearea;

import com.wanfadger.AdministrativeareaApi.entity.AdministrativeArea;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;

import java.util.List;
import java.util.Optional;

public interface DbAdministrativeAreaService {

    String generateCode();

    AdministrativeArea dbNew(AdministrativeArea administrativeArea);
    List<AdministrativeArea> dbNew(List<AdministrativeArea> administrativeAreas);

    List<AdministrativeArea> dbAllByPartOf(String partOf);
    List<AdministrativeArea> dbAllByAdministrativeType(AdministrativeAreaType administrativeAreaType);

    List<AdministrativeArea> dbAllByCodeLike(String code);
    List<AdministrativeArea> dbAllByNameLike(String name);


    Optional<AdministrativeArea> dbByName_Type_PartOf(String name , AdministrativeAreaType administrativeAreaType , String partOf);

    Optional<AdministrativeArea> dbByCode(String code);
    Optional<AdministrativeArea> dbByName(String name);



}
