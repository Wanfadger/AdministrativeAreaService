package com.wanfadger.AdministrativeareaApi.service.administrativearea;

import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaDtos;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeArea;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.repository.AdministrativeAreaRepository;
import com.wanfadger.AdministrativeareaApi.shared.administrativeareaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.shared.administrativeareaexceptions.MissingDataException;
import com.wanfadger.AdministrativeareaApi.shared.administrativeareaexceptions.NotFoundException;
import com.wanfadger.AdministrativeareaApi.shared.reponses.AdministrativeAreaResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AdministrativeAreaServiceImpl implements DbAdministrativeAreaService , AdministrativeAreaService{

    private final AdministrativeAreaRepository administrativeAreaRepository;


    @Override
    public ResponseEntity<AdministrativeAreaResponseDto<String>> newOne(AdministrativeAreaDtos.NewAdministrativeAreaDto dto) {
        Optional<AdministrativeAreaType> optionalAdministrativeAreaType = AdministrativeAreaType.administrativeAreaTypeStr(dto.getAdministrativeAreaType());
        if (optionalAdministrativeAreaType.isEmpty()) {
            throw new MissingDataException("Missing Administrative Area Type");
        }
        AdministrativeAreaType administrativeAreaType = optionalAdministrativeAreaType.get();
        Optional<AdministrativeArea> optionalAdministrativeArea = dbByName_Type_PartOf(dto.getName(), administrativeAreaType, dto.getPartOf());

        if (optionalAdministrativeArea.isEmpty()) {
            return new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success" , "Administrative area already exits") , HttpStatus.ALREADY_REPORTED);
        }


        AdministrativeArea administrativeArea = new AdministrativeArea();
        administrativeArea.setName(dto.getName());
        administrativeArea.setPartOf(dto.getPartOf());
        administrativeArea.setLatitude(dto.getLatitude() != null ? Double.valueOf(dto.getLatitude()) : null);
        administrativeArea.setLongitude(dto.getLongitude() !=null ? Double.valueOf(dto.getLongitude()) : null);


        administrativeArea.setAdministrativeAreaType(administrativeAreaType);

        // generate new code
        String code = generateCode();
        administrativeArea.setCode(code);

        dbNew(administrativeArea);
        return new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success" , "success") , HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<AdministrativeAreaResponseDto<String>> newList(List<AdministrativeAreaDtos.NewAdministrativeAreaDto> dtos) {

        // check if all have type
        if (dtos.parallelStream().anyMatch(dto -> AdministrativeAreaType.administrativeAreaTypeStr(dto.getAdministrativeAreaType()).isEmpty())) {
            throw new MissingDataException("Found Administrative Area without Type");
        }

        // exclude existing ones
        List<AdministrativeArea> administrativeAreaList = dtos.parallelStream().filter(dto -> {
            AdministrativeAreaType administrativeAreaType = AdministrativeAreaType.administrativeAreaTypeStr(dto.getAdministrativeAreaType()).get();
            return dbByName_Type_PartOf(dto.getName(), administrativeAreaType, dto.getPartOf()).isEmpty();
        }).map(dto -> {
            AdministrativeArea administrativeArea = new AdministrativeArea();
            administrativeArea.setName(dto.getName());
            administrativeArea.setPartOf(dto.getPartOf());
            administrativeArea.setLatitude(dto.getLatitude() != null ? Double.valueOf(dto.getLatitude()) : null);
            administrativeArea.setLongitude(dto.getLongitude() != null ? Double.valueOf(dto.getLongitude()) : null);

            AdministrativeAreaType administrativeAreaType = AdministrativeAreaType.administrativeAreaTypeStr(dto.getAdministrativeAreaType()).get();
            administrativeArea.setAdministrativeAreaType(administrativeAreaType);

            // generate new code
            String code = generateCode();
            administrativeArea.setCode(code);

            return administrativeArea;
        }).toList();


        dbNew(administrativeAreaList);
        return new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success" , "successfully added "+administrativeAreaList.size()+" administrative areas") , HttpStatus.CREATED);
    }


    @Override
    public AdministrativeAreaResponseDto<AdministrativeAreaDtos.AdministrativeAreaDto> filterOne(Map<String, String> queryMap) {
        String name = queryMap.get("name");
        String code = queryMap.get("code");

        if (name != null){
            AdministrativeArea administrativeArea = dbByName(name).orElseThrow(() -> new NotFoundException("Administrative Area not found"));

            return new AdministrativeAreaResponseDto<>(convertToDto1(administrativeArea));
        }

        if (code != null){
            AdministrativeArea administrativeArea = dbByCode(code).orElseThrow(() -> new NotFoundException("Administrative Area not found"));
            return new AdministrativeAreaResponseDto<>(convertToDto1(administrativeArea));
        }
        return null;
    }

    @Override
    public AdministrativeAreaResponseDto<List<AdministrativeAreaDtos.AdministrativeAreaDto>> filterList(Map<String, String> queryMap) {
        String type = queryMap.get("type");
        String partOf = queryMap.get("partOf");
        String name = queryMap.get("name");
        String code = queryMap.get("code");


        if (partOf != null) {
            List<AdministrativeAreaDtos.AdministrativeAreaDto> administrativeAreaDtos = dbAllByPartOf(partOf).parallelStream().map(this::convertToDto1).sorted(Comparator.comparing(AdministrativeAreaDtos.AdministrativeAreaDto::getName)).toList();
            return new AdministrativeAreaResponseDto<>(administrativeAreaDtos);
        }

        if (type != null) {
            AdministrativeAreaType administrativeAreaType = AdministrativeAreaType.administrativeAreaTypeStr(type).orElseThrow(() -> new InvalidException("Invalid type " + type));
            List<AdministrativeAreaDtos.AdministrativeAreaDto> administrativeAreaDtos = dbAllByAdministrativeType(administrativeAreaType).parallelStream().map(this::convertToDto1).sorted(Comparator.comparing(AdministrativeAreaDtos.AdministrativeAreaDto::getName)).toList();
            return new AdministrativeAreaResponseDto<>(administrativeAreaDtos);
        }


        if (name != null){
            List<AdministrativeAreaDtos.AdministrativeAreaDto> administrativeAreaDtos = dbAllByNameLike(name).parallelStream().map(this::convertToDto1).sorted(Comparator.comparing(AdministrativeAreaDtos.AdministrativeAreaDto::getName)).toList();
            return new AdministrativeAreaResponseDto<>(administrativeAreaDtos);
        }

        if (code != null){
            List<AdministrativeAreaDtos.AdministrativeAreaDto> administrativeAreaDtos = dbAllByCodeLike(code).parallelStream().map(this::convertToDto1).sorted(Comparator.comparing(AdministrativeAreaDtos.AdministrativeAreaDto::getName)).toList();
            return new AdministrativeAreaResponseDto<>(administrativeAreaDtos);
        }


        return new AdministrativeAreaResponseDto<>(Collections.emptyList());
    }



    private AdministrativeAreaDtos.AdministrativeAreaDto convertToDto1(AdministrativeArea administrativeArea){
        AdministrativeAreaDtos.AdministrativeAreaDto dto = new AdministrativeAreaDtos.AdministrativeAreaDto();
        dto.setCode(administrativeArea.getCode());
        dto.setPartOf(administrativeArea.getPartOf());
        dto.setName(administrativeArea.getName());
        dto.setLongitude(administrativeArea.getLongitude() != null ? String.valueOf(administrativeArea.getLongitude()) : null);
        dto.setLatitude(administrativeArea.getLatitude() != null ? String.valueOf(administrativeArea.getLatitude()) : null);
        return dto;
    }

    @Override
    public String generateCode() {
        String code;
        do{
            code = UUID.randomUUID().toString();
        }while (administrativeAreaRepository.findByCodeIgnoreCase(code).isPresent());
        return code;
    }

    @Override
    public AdministrativeArea dbNew(AdministrativeArea administrativeArea) {
        return administrativeAreaRepository.save(administrativeArea);
    }

    @Override
    public List<AdministrativeArea> dbNew(List<AdministrativeArea> administrativeAreas) {
        return administrativeAreaRepository.saveAll(administrativeAreas);
    }

    @Override
    public List<AdministrativeArea> dbAllByPartOf(String partOf) {
        return administrativeAreaRepository.findByPartOf(partOf);
    }

    @Override
    public List<AdministrativeArea> dbAllByAdministrativeType(AdministrativeAreaType administrativeAreaType) {
        return administrativeAreaRepository.findByAdministrativeAreaType(administrativeAreaType);
    }

    @Override
    public List<AdministrativeArea> dbAllByCodeLike(String code) {
        return administrativeAreaRepository.findAllByCodeLikeIgnoreCase(code);
    }

    @Override
    public List<AdministrativeArea> dbAllByNameLike(String name) {
        return administrativeAreaRepository.findAllByNameLikeIgnoreCase(name);
    }

    @Override
    public Optional<AdministrativeArea> dbByName_Type_PartOf(String name, AdministrativeAreaType administrativeAreaType, String partOf) {
        return administrativeAreaRepository.findByNameIgnoreCaseAndAdministrativeAreaTypeAndPartOf(name , administrativeAreaType , partOf);
    }

    @Override
    public Optional<AdministrativeArea> dbByCode(String code) {
        return administrativeAreaRepository.findByCodeIgnoreCase(code);
    }

    @Override
    public Optional<AdministrativeArea> dbByName(String name) {
        return administrativeAreaRepository.findByNameIgnoreCase(name);
    }
}
