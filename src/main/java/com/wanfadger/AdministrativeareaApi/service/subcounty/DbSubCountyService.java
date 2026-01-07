package com.wanfadger.AdministrativeareaApi.service.subcounty;


import com.wanfadger.AdministrativeareaApi.entity.SubCounty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface DbSubCountyService {
    SubCounty dbNew(SubCounty subCounty);
    List<SubCounty> dbNew(List<SubCounty> subCounties);

    List<SubCounty> dbList();
    Page<SubCounty> dbList(Pageable pageable);
    List<SubCounty> dbByCountyCode(String countyCode);
    List<SubCounty> dbByCountyCodes(List<String> countyCodes);

    Optional<SubCounty> dbByName_CountyCode(String name , String countyCode);

    Optional<SubCounty> dbByCode(String code);

}
