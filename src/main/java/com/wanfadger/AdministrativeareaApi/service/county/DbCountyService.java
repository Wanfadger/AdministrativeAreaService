package com.wanfadger.AdministrativeareaApi.service.county;


import com.wanfadger.AdministrativeareaApi.entity.County;
import com.wanfadger.AdministrativeareaApi.repository.projections.CodeNameProjection;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DbCountyService {

    County dbNew(County county);
    List<County> dbNew(List<County> counties);

    List<County> dbList();
    List<County> dbAllByLocalGovernmentCode(String code);

    Optional<County> dbByName_LocalGovernment_Code(String name , String localGovernmentCode);
    Optional<County> dbByCode(String code);

    Collection<CodeNameProjection> dbCodeNameList(Map<String , String> map);


    Optional<CodeNameProjection> dbCodeName(Map<String, String> queryMap);
}
