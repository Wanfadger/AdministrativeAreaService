package com.wanfadger.AdministrativeareaApi.service.localgovernment;


import com.wanfadger.AdministrativeareaApi.entity.LocalGovernment;

import java.util.List;
import java.util.Optional;

public interface DbLocalGovernmentService {

    LocalGovernment dbNew(LocalGovernment localGovernment);
    List<LocalGovernment> dbNew(List<LocalGovernment> localGovernments);

    List<LocalGovernment> dbList();
    List<LocalGovernment> dbBySubRegionCode(String subRegionCode);
    List<LocalGovernment> dbBySubRegionCodes(List<String> subRegionCodes);

    Optional<LocalGovernment> dbByName_SubRegionCode(String name , String subRegionCode);
    Optional<LocalGovernment> dbByCode(String code);

}
