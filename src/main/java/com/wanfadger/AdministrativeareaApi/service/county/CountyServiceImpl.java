package com.wanfadger.AdministrativeareaApi.service.county;

import com.wanfadger.AdministrativeareaApi.entity.County;
import com.wanfadger.AdministrativeareaApi.entity.LocalGovernment;
import com.wanfadger.AdministrativeareaApi.repository.CountyRepository;
import com.wanfadger.AdministrativeareaApi.repository.projections.CodeNameProjection;
import com.wanfadger.AdministrativeareaApi.shared.administrativeareaexceptions.AlreadyExistsException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CountyServiceImpl implements DbCountyService  {

    final CountyRepository countyRepository;

    @Override
    public County dbNew(@NonNull County county) {
        return countyRepository.save(county);
    }

    @Override
    public List<County> dbNew(List<County> counties) {
        return countyRepository.saveAll(counties);
    }

    @Override
    public List<County> dbList() {
        return countyRepository.findAll();
    }

    @Override
    public List<County> dbAllByLocalGovernmentCode(String code) {
        return countyRepository.findAllByLocalGovernment_Code(code);
    }

    @Override
    public Optional<County> dbByName_LocalGovernment_Code(String name, String localGovernmentCode) {
        return countyRepository.findByNameIgnoreCaseAndLocalGovernment_Code( name ,localGovernmentCode);
    }

    @Override
    public Optional<County> dbByCode(String code) {
        return Optional.empty();
    }




    @Override
    public List<CodeNameProjection> dbCodeNameList(Map<String , String> map) {
        return countyRepository.dbCodeNameList(map.get("partOf"));
    }

    @Override
    public Optional<CodeNameProjection> dbCodeName(Map<String, String> queryMap) {
        return countyRepository.dbCodeName(queryMap.get("code"));
    }

//    @Override
//    @Transactional
//    public SiipResponseDto<CountyDtos.CountyDto> newCounty(CountyDtos.NewDto dto) {
//        Optional<County> countyOptional = dbByName_LocalGovernmentId(dto.getName(), dto.getLocalGovernment().getId());
//        if (countyOptional.isPresent()) {
//            throw new AlreadyExistsException("Already Exists");
//        }
//
//        County county = new County();
//        county.setCode(dto.getCode());
//        county.setName(dto.getName());
//        county.setDescription(dto.getDescription());
//        county.setLocalGovernment(new LocalGovernment(dto.getLocalGovernment().getId()));
//
//        County dbNew = dbNew(county);
//
//        CountyDtos.CountyDto newDto = convertToDto(dbNew);
//
//        return new SiipResponseDto<>(newDto , "success");
//    }
//
//    private  CountyDtos.CountyDto convertToDto(County dbNew) {
//        CountyDtos.CountyDto dto = new CountyDtos.CountyDto();
//        dto.setId(dbNew.getId());
//        dto.setDescription(dbNew.getDescription());
//        dto.setName(dbNew.getName());
//        dto.setCode(dbNew.getCode());
//        LocalGovernment localGovernment = dbNew.getLocalGovernment();
//        if(localGovernment != null){
//            LocalGovernmentDtos.LocalGovernmentDto localGovernmentDto = new LocalGovernmentDtos.LocalGovernmentDto(localGovernment.getId(), localGovernment.getName());
//
//            Region region = localGovernment.getRegion();
//            if (region != null){
//                localGovernmentDto.setRegion(new RegionDtos.RegionDto(region.getId() , region.getName()));
//            }
//            dto.setLocalGovernment(localGovernmentDto);
//        }
//
//        return dto;
//    }
//
//    @Override
//    public SiipResponseDto<List<CountyDtos.CountyDto>> counties(Map<String  ,String> queryMap) {
//
//        String localGovernment = queryMap.get("localGovernment");
//        if(localGovernment != null && !localGovernment.isEmpty()){
//            List<CountyDtos.CountyDto> countyDtos = dbByLocalGovernmentId(localGovernment).stream().map(this::convertToDto).toList();
//            return new SiipResponseDto<>(countyDtos , "success");
//        }
//
//
//        List<CountyDtos.CountyDto> countyDtos = dbList().stream().map(this::convertToDto).toList();
//        return new SiipResponseDto<>(countyDtos , "succcess");
//    }
//
//    @Override
//    public SiipResponseDto<List<CountyDtos.CountyDto>> getAllByLocalGovernment(String localGovernmentId) {
//        List<CountyDtos.CountyDto> countyDtos = dbByLocalGovernmentId(localGovernmentId).stream().map(this::convertToDto).toList();
//        return new SiipResponseDto<>(countyDtos , "succcess");
//    }
//
//    @Override
//    @Transactional
//    public SiipResponseDto<String> updateCounty(String countyId, CountyDtos.NewDto dto) {
//        County county = dbById(countyId).orElseThrow(() -> new NotFoundException("County not found"));
//
//        if(notNullEmpty(dto.getName())){
//            county.setName(dto.getName());
//        }
//
//        if(notNullEmpty(dto.getCode())){
//            county.setCode(dto.getCode());
//        }
//
//        if(notNullEmpty(dto.getDescription())){
//            county.setDescription(dto.getDescription());
//        }
//
//        if(dto.getLocalGovernment() != null){
//            county.setLocalGovernment(new LocalGovernment(dto.getLocalGovernment().getId()));
//        }
//
//        dbNew(county);
//        return new SiipResponseDto<>("success" , "success");
//    }
//
//    private  boolean notNullEmpty(String value) {
//        return value != null && !value.isEmpty();
//    }
}
