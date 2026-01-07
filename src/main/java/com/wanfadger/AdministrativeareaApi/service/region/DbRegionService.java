package com.wanfadger.AdministrativeareaApi.service.region;


import com.wanfadger.AdministrativeareaApi.entity.Region;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DbRegionService {

    Region dbNew(Region region);
    List<Region> dbNew(List<Region> regions);

    List<Region> dbList();
    Page<Region> dbList(Pageable pageable);

    Optional<Region> dbByName(String name);
    Optional<Region> dbByCode(String code);
}
