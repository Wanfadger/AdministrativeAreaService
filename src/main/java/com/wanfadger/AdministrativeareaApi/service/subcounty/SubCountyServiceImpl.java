package com.wanfadger.AdministrativeareaApi.service.subcounty;



import com.wanfadger.AdministrativeareaApi.entity.SubCounty;
import com.wanfadger.AdministrativeareaApi.repository.SubCountyRepository;
import com.wanfadger.AdministrativeareaApi.repository.projections.CodeNameProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubCountyServiceImpl implements DbSubCountyService  {

    final SubCountyRepository subCountyRepository;

    @Override
    public SubCounty dbNew(SubCounty subCounty) {
        return subCountyRepository.save(subCounty);
    }

    @Override
    public List<SubCounty> dbNew(List<SubCounty> subCounties) {
        return subCountyRepository.saveAll(subCounties);
    }

    @Override
    public List<SubCounty> dbList() {
        return subCountyRepository.findAll();
    }

    @Override
    public List<SubCounty> dbByCountyCode(String countyId) {
        return subCountyRepository.findAllByCounty_Code(countyId);
    }

    @Override
    public Optional<SubCounty> dbByName_CountyCode(String name, String countyCode) {
        return subCountyRepository.findByNameIgnoreCaseAndCounty_Id(name , countyCode);
    }

    @Override
    public Optional<SubCounty> dbByCode(String code) {
        return subCountyRepository.findByCodeIgnoreCase(code);
    }


    @Override
    public List<CodeNameProjection> dbCodeNameList(Map<String , String> map) {
        return subCountyRepository.dbCodeNameList(map.get("partOf"));
    }

    @Override
    public Optional<CodeNameProjection> dbCodeName(Map<String, String> queryMap) {
        return subCountyRepository.dbCodeName(queryMap.get("code"));
    }
//
//    @Override
//    public SiipResponseDto<SubCountyDtos.SubCountyDto> newSubCounty(SubCountyDtos.NewDto dto) {
//        Optional<SubCounty> subCountyOptiona = dbByName_CountyId(dto.getName(), dto.getCounty().getId());
//        if (subCountyOptiona.isPresent()) {
//            throw new AlreadyExistsException("Already Exists");
//        }
//
//        SubCounty subCounty = new SubCounty();
//        subCounty.setCode(dto.getCode());
//        subCounty.setName(dto.getName());
//        subCounty.setDescription(dto.getDescription());
//        subCounty.setCounty(new County(dto.getCounty().getId()));
//
//        SubCounty dbNew = dbNew(subCounty);
//
//        SubCountyDtos.SubCountyDto newDto = convertToDto(dbNew);
//
//        return new SiipResponseDto<>(newDto , "success");
//    }
//
//    private  SubCountyDtos.SubCountyDto convertToDto(SubCounty dbNew) {
//        SubCountyDtos.SubCountyDto dto = new SubCountyDtos.SubCountyDto();
//        dto.setId(dbNew.getId());
//        dto.setDescription(dbNew.getDescription());
//        dto.setName(dbNew.getName());
//        dto.setCode(dbNew.getCode());
//        County county = dbNew.getCounty();
//
//        if (county != null){
//            CountyDtos.CountyDto countyDto = new CountyDtos.CountyDto(county.getId(), county.getName());
//
//            LocalGovernment localGovernment = county.getLocalGovernment();
//            if(localGovernment != null){
//                LocalGovernmentDtos.LocalGovernmentDto localGovernmentDto = new LocalGovernmentDtos.LocalGovernmentDto(localGovernment.getId(), localGovernment.getName());
//
//                Region region = localGovernment.getRegion();
//                if (region != null){
//                    localGovernmentDto.setRegion(new RegionDtos.RegionDto(region.getId() , region.getName()));
//                }
//
//                countyDto.setLocalGovernment(localGovernmentDto);
//            }
//
//            dto.setCounty(countyDto);
//        }
//
//        return dto;
//    }
//
//    @Override
//    public SiipResponseDto<List<SubCountyDtos.SubCountyDto>> subCounties(Map<String  ,String> queryMap) {
//
//        String county = queryMap.get("county");
//        if(county != null && !county.isEmpty()){
//            List<SubCountyDtos.SubCountyDto> subCountyDtos = dbByCountyId(county).stream().map(this::convertToDto).toList();
//            return new SiipResponseDto<>(subCountyDtos , "success");
//        }
//
//        List<SubCountyDtos.SubCountyDto> subCountyDtos = dbList().stream().map(this::convertToDto).toList();
//        return new SiipResponseDto<>(subCountyDtos , "success");
//    }
//
//    @Override
//    @Transactional
//    public SiipResponseDto<String> updateSubCounty(String subCountyId, SubCountyDtos.NewDto dto) {
//        SubCounty subCounty = dbById(subCountyId).orElseThrow(() -> new NotFoundException("SUbCounty not found"));
//
//        if(notNullEmpty(dto.getName())){
//            subCounty.setName(dto.getName());
//        }
//
//        if(notNullEmpty(dto.getCode())){
//            subCounty.setCode(dto.getCode());
//        }
//
//        if(notNullEmpty(dto.getDescription())){
//            subCounty.setDescription(dto.getDescription());
//        }
//
//        if(dto.getCounty() != null){
//            subCounty.setCounty(new County(dto.getCounty().getId()));
//        }
//
//        dbNew(subCounty);
//        return new SiipResponseDto<>("success" , "success");
//    }
//
//    private  boolean notNullEmpty(String value) {
//        return value != null && !value.isEmpty();
//    }
}
