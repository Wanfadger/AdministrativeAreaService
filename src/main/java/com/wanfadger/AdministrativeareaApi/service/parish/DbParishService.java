package com.wanfadger.AdministrativeareaApi.service.parish;


import com.wanfadger.AdministrativeareaApi.entity.Parish;

import java.util.List;
import java.util.Optional;

public interface DbParishService {
    Parish dbNew(Parish parish);
    List<Parish> dbNew(List<Parish> parishes);
    List<Parish> dbList();
    List<Parish> dbBySubCountyCode(String code);
    List<Parish> dbBySubCountyCodes(List<String> codes);

    Optional<Parish> dbByName_SubCountyCode(String name , String subCountyCode);
    Optional<Parish> dbByCode(String subCountyCode);

}
