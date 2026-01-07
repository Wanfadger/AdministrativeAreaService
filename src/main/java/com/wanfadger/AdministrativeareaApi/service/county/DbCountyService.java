package com.wanfadger.AdministrativeareaApi.service.county;


import com.wanfadger.AdministrativeareaApi.entity.County;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface DbCountyService {

    County dbNew(County county);
    List<County> dbNew(List<County> counties);

    List<County> dbList();
    Page<County> dbList(Pageable pageable);
    List<County> dbAllByLocalGovernmentCode(String code);
    List<County> dbAllByLocalGovernmentCodes(List<String> codeS);

    Optional<County> dbByName_LocalGovernment_Code(String name , String localGovernmentCode);
    Optional<County> dbByCode(String code);


}
