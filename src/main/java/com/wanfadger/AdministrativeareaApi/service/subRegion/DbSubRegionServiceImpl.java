package com.wanfadger.AdministrativeareaApi.service.subRegion;

import com.wanfadger.AdministrativeareaApi.entity.SubRegion;
import com.wanfadger.AdministrativeareaApi.repository.SubRegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DbSubRegionServiceImpl implements DbSubRegionService{
    final SubRegionRepository subRegionRepository;


    @Override
    public SubRegion dbNew(SubRegion subRegion) {
        return subRegionRepository.save(subRegion);
    }

    @Override
    public List<SubRegion> dbNew(List<SubRegion> subRegions) {
        return subRegionRepository.saveAll(subRegions);
    }

    @Override
    public List<SubRegion> dbList() {
        return subRegionRepository.findAll();
    }

    @Override
    public List<SubRegion> dbByRegionCode(String regionCode) {
        return subRegionRepository.findAllByRegion_Code(regionCode);
    }



    @Override
    public Optional<SubRegion> dbByName_RegionCode(String name, String regionCode) {
        return subRegionRepository.findByNameIgnoreCaseAndRegion_Code(name, regionCode);
    }

    @Override
    public Optional<SubRegion> dbByCode(String code) {
        return subRegionRepository.findByCodeIgnoreCase(code);
    }




}
