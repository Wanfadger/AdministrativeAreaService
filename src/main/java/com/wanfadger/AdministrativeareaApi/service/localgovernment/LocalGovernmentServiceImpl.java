package com.wanfadger.AdministrativeareaApi.service.localgovernment;


import com.wanfadger.AdministrativeareaApi.entity.LocalGovernment;
import com.wanfadger.AdministrativeareaApi.repository.LocalGovernmentRepository;
import com.wanfadger.AdministrativeareaApi.repository.projections.CodeNameProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LocalGovernmentServiceImpl implements DbLocalGovernmentService  {

    final LocalGovernmentRepository localGovernmentRepository;

    @Override
    public LocalGovernment dbNew(LocalGovernment localGovernment) {
        return localGovernmentRepository.save(localGovernment);
    }

    @Override
    public List<LocalGovernment> dbNew(List<LocalGovernment> localGovernments) {
        return localGovernmentRepository.saveAll(localGovernments);
    }

    @Override
    public List<LocalGovernment> dbList() {
        return localGovernmentRepository.findAll();
    }

    @Override
    public List<LocalGovernment> dbBySubRegionCode(String subRegionCode) {
        return localGovernmentRepository.findAllBySubRegion_Code(subRegionCode);
    }

    @Override
    public List<LocalGovernment> dbBySubRegionCodes(List<String> subRegionCodes) {
        return localGovernmentRepository.findAllBySubRegionCodes(subRegionCodes);
    }

    @Override
    public Optional<LocalGovernment> dbByName_SubRegionCode(String name, String subRegionCode) {
        return localGovernmentRepository.findByNameIgnoreCaseAndSubRegion_Code(name, subRegionCode);
    }

    @Override
    public Optional<LocalGovernment> dbByCode(String code) {
        return localGovernmentRepository.findByCodeIgnoreCase(code);
    }

    @Override
    public List<CodeNameProjection> dbCodeNameList(Map<String, String> map) {
        return localGovernmentRepository.dbCodeNameList(map.get("partOf"));
    }

    @Override
    public Optional<CodeNameProjection> dbCodeName(Map<String, String> queryMap) {
        return localGovernmentRepository.dbCodeName(queryMap.get("code"));
    }


//    @Override
//    @Transactional
//    public SiipResponseDto<LocalGovernmentDtos.LocalGovernmentDto> newLocalGovernment(LocalGovernmentDtos.NewDto dto) {
//
//        Optional<LocalGovernment> localGovernmentOptional = dbByName_RegionId(dto.getName(), dto.getRegion().getId());
//        if (localGovernmentOptional.isPresent()) {
//            throw new AlreadyExistsException("Already Exists");
//        }
//
//        LocalGovernment localGovernment = new LocalGovernment();
//        localGovernment.setCode(dto.getCode());
//        localGovernment.setName(dto.getName());
//        localGovernment.setDescription(dto.getDescription());
//        localGovernment.setRegion(new Region(dto.getRegion().getId()));
//
//        LocalGovernment dbNew = dbNew(localGovernment);
//
//        LocalGovernmentDtos.LocalGovernmentDto newDto = convertToDto(dbNew);
//
//        return new SiipResponseDto<>(newDto , "success");
//    }
//
//    private  LocalGovernmentDtos.LocalGovernmentDto convertToDto(LocalGovernment dbNew) {
//        LocalGovernmentDtos.LocalGovernmentDto newDto = new LocalGovernmentDtos.LocalGovernmentDto();
//        newDto.setId(dbNew.getId());
//        newDto.setDescription(dbNew.getDescription());
//        newDto.setName(dbNew.getName());
//        newDto.setCode(dbNew.getCode());
//        newDto.setRegion(new RegionDtos.RegionDto(dbNew.getRegion().getId() , dbNew.getRegion().getName()));
//        return newDto;
//    }
//
//    @Override
//    public SiipResponseDto<List<LocalGovernmentDtos.LocalGovernmentDto>> localGovernments(Map<String  ,String> queryMap) {
//        String region = queryMap.get("region");
//        if(region != null && !region.isEmpty()){
//            List<LocalGovernmentDtos.LocalGovernmentDto> localGovernmentDtos = dbByRegionId(region).stream().map(this::convertToDto).toList();
//            return new SiipResponseDto<>(localGovernmentDtos , "success");
//        }
//
//
//        List<LocalGovernmentDtos.LocalGovernmentDto> localGovernmentDtos = dbList().stream().map(this::convertToDto).toList();
//        return new SiipResponseDto<>(localGovernmentDtos , "success");
//    }
//
//    @Override
//    @Transactional
//    public SiipResponseDto<String> updateLocalGovernment(String localGovernmentId, LocalGovernmentDtos.NewDto dto) {
//        LocalGovernment localGovernment = dbById(localGovernmentId).orElseThrow(() -> new NotFoundException("Local governmet not found"));
//
//        if(notNullEmpty(dto.getName())){
//            localGovernment.setName(dto.getName());
//        }
//
//        if(notNullEmpty(dto.getCode())){
//            localGovernment.setCode(dto.getCode());
//        }
//
//        if(notNullEmpty(dto.getDescription())){
//            localGovernment.setDescription(dto.getDescription());
//        }
//
//        if(dto.getRegion() != null){
//            localGovernment.setRegion(new Region(dto.getRegion().getId()));
//        }
//
//        dbNew(localGovernment);
//        return new SiipResponseDto<>("success" , "success");
//    }
//
//    private boolean notNullEmpty(String value) {
//        return value != null && !value.isEmpty();
//    }


}
