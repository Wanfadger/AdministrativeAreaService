package com.wanfadger.AdministrativeareaApi.service.subRegion;


import com.wanfadger.AdministrativeareaApi.entity.SubRegion;

import java.util.List;
import java.util.Optional;

public interface DbSubRegionService {

    SubRegion dbNew(SubRegion subRegion);
    List<SubRegion> dbNew(List<SubRegion> subRegions);

    List<SubRegion> dbList();
    List<SubRegion> dbByRegionCode(String regionCode);


    Optional<SubRegion> dbByName_RegionCode(String name , String regionCode);
    Optional<SubRegion> dbByCode(String code);



}
