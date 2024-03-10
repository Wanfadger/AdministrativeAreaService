package com.wanfadger.AdministrativeareaApi.service.subRegion;



import com.wanfadger.AdministrativeareaApi.entity.SubRegion;
import com.wanfadger.AdministrativeareaApi.repository.projections.CodeNameProjection;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DbSubRegionService {

    SubRegion dbNew(SubRegion subRegion);
    List<SubRegion> dbNew(List<SubRegion> subRegions);

    List<SubRegion> dbList();
    List<SubRegion> dbByRegionCode(String regionCode);

    Optional<SubRegion> dbByName_RegionCode(String name , String regionCode);
    Optional<SubRegion> dbByCode(String code);

    List<CodeNameProjection> dbCodeNameList(Map<String , String> map);
    Optional<CodeNameProjection> dbCodeName(Map<String , String> map);


}
