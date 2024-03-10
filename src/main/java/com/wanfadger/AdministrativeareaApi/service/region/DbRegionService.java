package com.wanfadger.AdministrativeareaApi.service.region;


import com.wanfadger.AdministrativeareaApi.entity.Region;
import com.wanfadger.AdministrativeareaApi.repository.projections.CodeNameProjection;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DbRegionService {

    Region dbNew(Region region);
    List<Region> dbNew(List<Region> regions);

    List<Region> dbList();

    Optional<Region> dbByName(String name);
    Optional<Region> dbByCode(String code);
    List<CodeNameProjection> dbCodeNameList();
    Optional<CodeNameProjection> dbCodeName(String code);
}
