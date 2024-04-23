package com.wanfadger.AdministrativeareaApi.service.subcounty;



import com.wanfadger.AdministrativeareaApi.entity.SubCounty;
import com.wanfadger.AdministrativeareaApi.repository.projections.CodeNameProjection;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DbSubCountyService {
    SubCounty dbNew(SubCounty subCounty);
    List<SubCounty> dbNew(List<SubCounty> subCounties);

    List<SubCounty> dbList();
    List<SubCounty> dbByCountyCode(String countyCode);
    List<SubCounty> dbByCountyCodes(List<String> countyCodes);

    Optional<SubCounty> dbByName_CountyCode(String name , String countyCode);

    Optional<SubCounty> dbByCode(String code);

    List<CodeNameProjection> dbCodeNameList(Map<String , String> map);


    Optional<CodeNameProjection> dbCodeName(Map<String, String> queryMap);
}
