package com.wanfadger.AdministrativeareaApi.service.region;


import com.wanfadger.AdministrativeareaApi.entity.Region;
import com.wanfadger.AdministrativeareaApi.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RegionServiceImpl implements DbRegionService  {

    final RegionRepository regionRepository;

//    @Override
//    @Transactional
//    public SiipResponseDto<RegionDtos.RegionDto> newRegion(RegionDtos.NewDto dto) {
//        Optional<Region> regionOptional = dbByName(dto.getName());
//        if (regionOptional.isPresent()) {
//            throw new AlreadyExistsException("Region already exists");
//        }
//
//        Region region = new Region();
//        region.setCode(dto.getCode());
//        region.setName(dto.getName());
//        region.setDescription(dto.getDescription());
//
//        Region dbNewRegion = dbNew(region);
//
//        RegionDtos.RegionDto regionDto = new RegionDtos.RegionDto(dbNewRegion.getId() , dbNewRegion.getCode(), dbNewRegion.getName(), dbNewRegion.getDescription());
//
//        return new SiipResponseDto<>(regionDto , "success");
//    }
//
//    @Override
//    public SiipResponseDto<List<RegionDtos.RegionDto>> regions() {
//        List<RegionDtos.RegionDto> dtoList = dbList().stream().map(region -> new RegionDtos.RegionDto(region.getId(), region.getCode(), region.getName(), region.getDescription())).toList();
//        return new SiipResponseDto<>(dtoList , "success");
//    }
//
//    @Override
//    @Transactional
//    public SiipResponseDto<String> updateRegion(String regionId, RegionDtos.NewDto dto) {
//        Region region = dbById(regionId).orElseThrow(() -> new NotFoundException("Region not found"));
//
//        if(notNullEmpty(dto.getName())){
//            region.setName(dto.getName());
//        }
//
//        if(notNullEmpty(dto.getCode())){
//            region.setCode(dto.getCode());
//        }
//
//        if(notNullEmpty(dto.getDescription())){
//            region.setDescription(dto.getDescription());
//        }
//
//        dbNew(region);
//
//        return new SiipResponseDto<>("success" , "success");
//    }


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
    public Optional<Region> dbByName(String name) {
        return regionRepository.findByNameIgnoreCase(name);
    }

    @Override
    public Optional<Region> dbByCode(String code) {
        return regionRepository.findByCodeIgnoreCase(code);
    }


}
