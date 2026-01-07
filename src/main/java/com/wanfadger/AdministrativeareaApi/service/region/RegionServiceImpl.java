package com.wanfadger.AdministrativeareaApi.service.region;


import com.wanfadger.AdministrativeareaApi.entity.Region;
import com.wanfadger.AdministrativeareaApi.repository.RegionRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RegionServiceImpl implements DbRegionService  {

    final RegionRepository regionRepository;


    @Override
    public Region dbNew(Region region) {
        return regionRepository.save(region);
    }

    @Override
    public List<Region> dbNew(List<Region> regions) {
        return regionRepository.saveAll(regions);
    }

    @Override
    public List<Region> dbList() {
        return regionRepository.findAll();
    }

    @Override
    public Page<Region> dbList(Pageable pageable) {
        return regionRepository.findAll(pageable);
    }

    @Override
    public Optional<Region> dbByName(String name) {
        return regionRepository.findByNameIgnoreCase(name);
    }

    @Override
    public Optional<Region> dbByCode(String code) {
        return regionRepository.findByCodeIgnoreCase(code);
    }




}
