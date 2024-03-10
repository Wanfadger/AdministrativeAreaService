package com.wanfadger.AdministrativeareaApi.service.parish;



import com.wanfadger.AdministrativeareaApi.entity.Parish;
import com.wanfadger.AdministrativeareaApi.repository.projections.CodeNameProjection;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DbParishService {
    Parish dbNew(Parish parish);
    List<Parish> dbNew(List<Parish> parishes);
    List<Parish> dbList();
    List<Parish> dbBySubCountyCode(String code);

    Optional<Parish> dbByName_SubCountyCode(String name , String subCountyCode);
    Optional<Parish> dbByCode(String subCountyCode);

    List<CodeNameProjection> dbCodeNameList(Map<String , String> map);


    Optional<CodeNameProjection> dbCodeName(Map<String, String> queryMap);
}
