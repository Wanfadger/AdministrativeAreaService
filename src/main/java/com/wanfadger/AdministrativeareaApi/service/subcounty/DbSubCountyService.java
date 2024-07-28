package com.wanfadger.AdministrativeareaApi.service.subcounty;


import com.wanfadger.AdministrativeareaApi.entity.SubCounty;

import java.util.List;
import java.util.Optional;

public interface DbSubCountyService {
    SubCounty dbNew(SubCounty subCounty);
    List<SubCounty> dbNew(List<SubCounty> subCounties);

    List<SubCounty> dbList();
    List<SubCounty> dbByCountyCode(String countyCode);
    List<SubCounty> dbByCountyCodes(List<String> countyCodes);

    Optional<SubCounty> dbByName_CountyCode(String name , String countyCode);

    Optional<SubCounty> dbByCode(String code);

}
