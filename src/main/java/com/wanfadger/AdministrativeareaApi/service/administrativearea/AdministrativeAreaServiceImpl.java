package com.wanfadger.AdministrativeareaApi.service.administrativearea;

import com.wanfadger.AdministrativeareaApi.dto.*;
import com.wanfadger.AdministrativeareaApi.entity.*;
import com.wanfadger.AdministrativeareaApi.repository.projections.CodeNameProjection;
import com.wanfadger.AdministrativeareaApi.service.county.DbCountyService;
import com.wanfadger.AdministrativeareaApi.service.localgovernment.DbLocalGovernmentService;
import com.wanfadger.AdministrativeareaApi.service.parish.DbParishService;
import com.wanfadger.AdministrativeareaApi.service.region.DbRegionService;
import com.wanfadger.AdministrativeareaApi.service.subRegion.DbSubRegionService;
import com.wanfadger.AdministrativeareaApi.service.subcounty.DbSubCountyService;
import com.wanfadger.AdministrativeareaApi.shared.administrativeareaexceptions.AlreadyExistsException;
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
public class AdministrativeAreaServiceImpl implements AdministrativeAreaService {
    private final DbRegionService dbRegionService;
    private final DbSubRegionService dbSubRegionService;
    private final DbLocalGovernmentService dbLocalGovernmentService;
    private final DbCountyService dbCountyService;
    private final DbSubCountyService dbSubCountyService;
    private final DbParishService dbParishService;

    private boolean notNullEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    private boolean nullEmpty(String value) {
        return null == value;
    }


    private String generateCode(AdministrativeAreaType administrativeAreaType) {
        String code;
        return switch (administrativeAreaType) {
            case REGION -> {
                do {
                    code = UUID.randomUUID().toString();
                } while (dbRegionService.dbByCode(code).isPresent());
                yield code;
            }

            case SUBREGION -> {
                do {
                    code = UUID.randomUUID().toString();
                } while (dbSubRegionService.dbByCode(code).isPresent());
                yield code;
            }

            case LOCALGOVERNMENT -> {
                do {
                    code = UUID.randomUUID().toString();
                } while (dbLocalGovernmentService.dbByCode(code).isPresent());

                yield code;
            }

            case COUNTY -> {
                do {
                    code = UUID.randomUUID().toString();
                } while (dbCountyService.dbByCode(code).isPresent());
                yield code;
            }

            case SUBCOUNTY -> {
                do {
                    code = UUID.randomUUID().toString();
                } while (dbSubCountyService.dbByCode(code).isPresent());
                yield code;
            }

            case PARISH -> {
                do {
                    code = UUID.randomUUID().toString();
                } while (dbParishService.dbByCode(code).isPresent());
                yield code;
            }

        };
    }

    @Override
    public ResponseEntity<AdministrativeAreaResponseDto<String>> newOne(String type, NewAdministrativeAreaDto dto) {
        Optional<AdministrativeAreaType> optionalAdministrativeAreaType = AdministrativeAreaType.administrativeAreaTypeStr(type);
        if (optionalAdministrativeAreaType.isEmpty()) {
            throw new MissingDataException("Missing Administrative Area Type");
        }

        AdministrativeAreaType administrativeAreaType = optionalAdministrativeAreaType.get();

        return switch (administrativeAreaType) {
            case REGION -> {

                if (dbRegionService.dbByName(dto.getName()).isPresent()) {
                    throw new AlreadyExistsException("Administrative Area Already Exists");
                }


                Region region = convertDtoRegion(dto, administrativeAreaType);

                dbRegionService.dbNew(region);

                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success", "success"), HttpStatus.CREATED);
            }
            case SUBREGION -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode");
                }

                if (dbSubRegionService.dbByName_RegionCode(dto.getName(), dto.getPartOfCode()).isPresent()) {
                    throw new AlreadyExistsException("Administrative Area Already Exists");
                }

                SubRegion subRegion = convertDtoSubRegion(dto, administrativeAreaType);

                dbSubRegionService.dbNew(subRegion);
                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success", "success"), HttpStatus.CREATED);
            }
            case LOCALGOVERNMENT -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode");
                }

                if (dbLocalGovernmentService.dbByName_SubRegionCode(dto.getName(), dto.getPartOfCode()).isPresent()) {
                    throw new AlreadyExistsException("Administrative Area Already Exists");
                }

                LocalGovernment localGovernment = convertDtoLocalGovernment(dto, administrativeAreaType);

                dbLocalGovernmentService.dbNew(localGovernment);
                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success", "success"), HttpStatus.CREATED);
            }

            case COUNTY -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode");
                }

                if (dbCountyService.dbByName_LocalGovernment_Code(dto.getName(), dto.getPartOfCode()).isPresent()) {
                    throw new AlreadyExistsException("Administrative Area Already Exists");
                }

                County county = convertDtoCounty(dto, administrativeAreaType);

                dbCountyService.dbNew(county);
                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success", "success"), HttpStatus.CREATED);
            }

            case SUBCOUNTY -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode");
                }

                if (dbSubCountyService.dbByName_CountyCode(dto.getName(), dto.getPartOfCode()).isPresent()) {
                    throw new AlreadyExistsException("Administrative Area Already Exists");
                }

                SubCounty subCounty = convertDtoSubCounty(dto, administrativeAreaType);

                dbSubCountyService.dbNew(subCounty);
                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success", "success"), HttpStatus.CREATED);
            }

            case PARISH -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode");
                }

                if (dbParishService.dbByName_SubCountyCode(dto.getName(), dto.getPartOfCode()).isPresent()) {
                    throw new AlreadyExistsException("Administrative Area Already Exists");
                }

                Parish parish = convertDtoParish(dto, administrativeAreaType);

                dbParishService.dbNew(parish);
                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success", "success"), HttpStatus.CREATED);
            }
        };
    }

    private Parish convertDtoParish(NewAdministrativeAreaDto dto, AdministrativeAreaType administrativeAreaType) {
        Parish parish = new Parish();
        parish.setName(dto.getName());
        parish.setLatitude(dto.getLatitude() != null ? Double.valueOf(dto.getLatitude()) : null);
        parish.setLongitude(dto.getLongitude() != null ? Double.valueOf(dto.getLongitude()) : null);
        parish.setPartOfCode(dto.getPartOfCode());
        parish.setCode(generateCode(administrativeAreaType));
        return parish;
    }

    private SubCounty convertDtoSubCounty(NewAdministrativeAreaDto dto, AdministrativeAreaType administrativeAreaType) {
        SubCounty subCounty = new SubCounty();
        subCounty.setName(dto.getName());
        subCounty.setLatitude(dto.getLatitude() != null ? Double.valueOf(dto.getLatitude()) : null);
        subCounty.setLongitude(dto.getLongitude() != null ? Double.valueOf(dto.getLongitude()) : null);
        subCounty.setPartOfCode(dto.getPartOfCode());
        subCounty.setCode(generateCode(administrativeAreaType));
        return subCounty;
    }

    private County convertDtoCounty(NewAdministrativeAreaDto dto, AdministrativeAreaType administrativeAreaType) {
        County county = new County();
        county.setName(dto.getName());
        county.setLatitude(dto.getLatitude() != null ? Double.valueOf(dto.getLatitude()) : null);
        county.setLongitude(dto.getLongitude() != null ? Double.valueOf(dto.getLongitude()) : null);
        county.setPartOfCode(dto.getPartOfCode());
        county.setCode(generateCode(administrativeAreaType));
        return county;
    }

    private LocalGovernment convertDtoLocalGovernment(NewAdministrativeAreaDto dto, AdministrativeAreaType administrativeAreaType) {
        LocalGovernment localGovernment = new LocalGovernment();
        localGovernment.setName(dto.getName());
        localGovernment.setLatitude(dto.getLatitude() != null ? Double.valueOf(dto.getLatitude()) : null);
        localGovernment.setLongitude(dto.getLongitude() != null ? Double.valueOf(dto.getLongitude()) : null);
        localGovernment.setPartOfCode(dto.getPartOfCode());
        localGovernment.setCode(generateCode(administrativeAreaType));
        return localGovernment;
    }

    private SubRegion convertDtoSubRegion(NewAdministrativeAreaDto dto, AdministrativeAreaType administrativeAreaType) {
        SubRegion subRegion = new SubRegion();
        subRegion.setName(dto.getName());
        subRegion.setLatitude(dto.getLatitude() != null ? Double.valueOf(dto.getLatitude()) : null);
        subRegion.setLongitude(dto.getLongitude() != null ? Double.valueOf(dto.getLongitude()) : null);
        subRegion.setPartOfCode(dto.getPartOfCode());
        subRegion.setCode(generateCode(administrativeAreaType));
        return subRegion;
    }

    private Region convertDtoRegion(NewAdministrativeAreaDto dto, AdministrativeAreaType administrativeAreaType) {
        Region region = new Region();
        region.setName(dto.getName());
        region.setLatitude(dto.getLatitude() != null ? Double.valueOf(dto.getLatitude()) : null);
        region.setLongitude(dto.getLongitude() != null ? Double.valueOf(dto.getLongitude()) : null);
        region.setCode(generateCode(administrativeAreaType));
        return region;
    }

    @Override
    public ResponseEntity<AdministrativeAreaResponseDto<String>> newList(String type, List<NewAdministrativeAreaDto> dtos) {
        Optional<AdministrativeAreaType> optionalAdministrativeAreaType = AdministrativeAreaType.administrativeAreaTypeStr(type);
        if (optionalAdministrativeAreaType.isEmpty()) {
            throw new MissingDataException("Missing Administrative Area Type");
        }

        AdministrativeAreaType administrativeAreaType = optionalAdministrativeAreaType.get();

        return switch (administrativeAreaType) {
            case REGION -> {
                // exclude existing ones
                List<Region> regions = dtos.parallelStream().filter(dto -> dbRegionService.dbByName(dto.getName()).isEmpty()).map(dto -> {
                    Region region = convertDtoRegion(dto, administrativeAreaType);

                    return region;
                }).toList();


                dbRegionService.dbNew(regions);
                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success", "successfully added " + regions.size() + " administrative areas"), HttpStatus.CREATED);

            }
            case SUBREGION -> {

                // check if all have PartOfCode
                if (dtos.parallelStream().anyMatch(dto -> !notNullEmpty(dto.getPartOfCode()))) {
                    throw new MissingDataException("Found Administrative Area without PartOfCoce");
                }

                // exclude existing ones
                List<SubRegion> subRegions = dtos.parallelStream().filter(dto -> dbSubRegionService.dbByName_RegionCode(dto.getName(), dto.getPartOfCode()).isEmpty()).map(dto -> {
                    SubRegion subRegion = convertDtoSubRegion(dto, administrativeAreaType);
                    return subRegion;
                }).toList();


                dbSubRegionService.dbNew(subRegions);
                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success", "successfully added " + subRegions.size() + " administrative areas"), HttpStatus.CREATED);
            }
            case LOCALGOVERNMENT -> {

                // check if all have PartOfCode
                if (dtos.parallelStream().anyMatch(dto -> !notNullEmpty(dto.getPartOfCode()))) {
                    throw new MissingDataException("Found Administrative Area without PartOfCoce");
                }

                // exclude existing ones
                List<LocalGovernment> localGovernments = dtos.parallelStream().filter(dto -> dbLocalGovernmentService.dbByName_SubRegionCode(dto.getName(), dto.getPartOfCode()).isEmpty()).map(dto -> {
                    LocalGovernment localGovernment = convertDtoLocalGovernment(dto, administrativeAreaType);
                    return localGovernment;
                }).toList();


                dbLocalGovernmentService.dbNew(localGovernments);
                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success", "successfully added " + localGovernments.size() + " administrative areas"), HttpStatus.CREATED);
            }

            case COUNTY -> {

                // check if all have PartOfCode
                if (dtos.parallelStream().anyMatch(dto -> !notNullEmpty(dto.getPartOfCode()))) {
                    throw new MissingDataException("Found Administrative Area without PartOfCoce");
                }

                // exclude existing ones
                List<County> counties = dtos.parallelStream().filter(dto -> dbCountyService.dbByName_LocalGovernment_Code(dto.getName(), dto.getPartOfCode()).isEmpty()).map(dto -> {
                    County county = convertDtoCounty(dto, administrativeAreaType);
                    return county;
                }).toList();


                dbCountyService.dbNew(counties);
                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success", "successfully added " + counties.size() + " administrative areas"), HttpStatus.CREATED);
            }

            case SUBCOUNTY -> {

                // check if all have PartOfCode
                if (dtos.parallelStream().anyMatch(dto -> !notNullEmpty(dto.getPartOfCode()))) {
                    throw new MissingDataException("Found Administrative Area without PartOfCoce");
                }

                // exclude existing ones
                List<SubCounty> subCounties = dtos.parallelStream().filter(dto -> dbSubCountyService.dbByName_CountyCode(dto.getName(), dto.getPartOfCode()).isEmpty()).map(dto -> {
                    SubCounty subCounty = convertDtoSubCounty(dto, administrativeAreaType);
                    return subCounty;
                }).toList();


                dbSubCountyService.dbNew(subCounties);
                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success", "successfully added " + subCounties.size() + " administrative areas"), HttpStatus.CREATED);
            }

            case PARISH -> {

                // check if all have PartOfCode
                if (dtos.parallelStream().anyMatch(dto -> !notNullEmpty(dto.getPartOfCode()))) {
                    throw new MissingDataException("Found Administrative Area without PartOfCoce");
                }

                // exclude existing ones
                List<Parish> parishes = dtos.parallelStream().filter(dto -> dbParishService.dbByName_SubCountyCode(dto.getName(), dto.getPartOfCode()).isEmpty()).map(dto -> {
                    Parish parish = convertDtoParish(dto, administrativeAreaType);
                    return parish;
                }).toList();


                dbParishService.dbNew(parishes);
                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success", "successfully added " + parishes.size() + " administrative areas"), HttpStatus.CREATED);

            }
        };

    }

    @Override
    public AdministrativeAreaResponseDto<CodeNameProjection> filterOne(Map<String, String> queryMap) {
        String type = queryMap.get("type");
        String code = queryMap.get("code");

        if (!notNullEmpty(type)) {
            throw new MissingDataException("Missing Administrative Area Type");
        }


        Optional<AdministrativeAreaType> optionalAdministrativeAreaType = AdministrativeAreaType.administrativeAreaTypeStr(type);
        if (optionalAdministrativeAreaType.isEmpty()) {
            throw new MissingDataException("Missing Administrative Area Type");
        }

        AdministrativeAreaType administrativeAreaType = optionalAdministrativeAreaType.get();

        return switch (administrativeAreaType) {
            case REGION -> {
                if (!notNullEmpty(code)) {
                    throw new MissingDataException("Missing Administrative Area Code");
                }
                CodeNameProjection codeNameProjection = dbRegionService.dbCodeName(code).orElseThrow(() -> new NotFoundException("Region not found"));
                yield new AdministrativeAreaResponseDto<>(codeNameProjection);
            }
            case SUBREGION -> {
                if (!notNullEmpty(code)) {
                    throw new MissingDataException("Missing Administrative Area Code");
                }
                CodeNameProjection codeNameProjection = dbSubRegionService.dbCodeName(queryMap).orElseThrow(() -> new NotFoundException("SubRegion not found"));
                yield new AdministrativeAreaResponseDto<>(codeNameProjection);
            }
            case LOCALGOVERNMENT -> {
                if (!notNullEmpty(code)) {
                    throw new MissingDataException("Missing Administrative Area Code");
                }
                CodeNameProjection codeNameProjection = dbLocalGovernmentService.dbCodeName(queryMap).orElseThrow(() -> new NotFoundException("LocalGovernment not found"));
                yield new AdministrativeAreaResponseDto<>(codeNameProjection);
            }
            case COUNTY -> {
                if (!notNullEmpty(code)) {
                    throw new MissingDataException("Missing Administrative Area Code");
                }
                CodeNameProjection codeNameProjection = dbCountyService.dbCodeName(queryMap).orElseThrow(() -> new NotFoundException("County not found"));
                yield new AdministrativeAreaResponseDto<>(codeNameProjection);
            }
            case SUBCOUNTY -> {
                if (!notNullEmpty(code)) {
                    throw new MissingDataException("Missing Administrative Area Code");
                }
                CodeNameProjection codeNameProjection = dbSubCountyService.dbCodeName(queryMap).orElseThrow(() -> new NotFoundException("County not found"));
                yield new AdministrativeAreaResponseDto<>(codeNameProjection);
            }
            case PARISH -> {
                if (!notNullEmpty(code)) {
                    throw new MissingDataException("Missing Administrative Area Code");
                }
                CodeNameProjection codeNameProjection = dbParishService.dbCodeName(queryMap).orElseThrow(() -> new NotFoundException("County not found"));
                yield new AdministrativeAreaResponseDto<>(codeNameProjection);
            }
        };

    }

    @Override
    public AdministrativeAreaResponseDto<List<CodeNameProjection>> filterList(Map<String, String> queryMap) {
        String type = queryMap.get("type");
        String partOf = queryMap.get("partOf");


        if (!notNullEmpty(type)) {
            throw new MissingDataException("Missing Administrative Area Type");
        }


        Optional<AdministrativeAreaType> optionalAdministrativeAreaType = AdministrativeAreaType.administrativeAreaTypeStr(type);
        if (optionalAdministrativeAreaType.isEmpty()) {
            throw new MissingDataException("Missing Administrative Area Type");
        }

        AdministrativeAreaType administrativeAreaType = optionalAdministrativeAreaType.get();

        return switch (administrativeAreaType) {
            case REGION -> {
                List<CodeNameProjection> codeNameProjections = dbRegionService.dbCodeNameList().parallelStream()
                        .sorted(Comparator.comparing(CodeNameProjection::getCode)).toList();
                yield new AdministrativeAreaResponseDto<>(codeNameProjections);
            }

            case SUBREGION -> {

                if (!notNullEmpty(partOf)) {
                    throw new MissingDataException("Missing Administrative Area partOf");
                }

                List<CodeNameProjection> codeNameProjections = dbSubRegionService.dbCodeNameList(queryMap).parallelStream()
                        .sorted(Comparator.comparing(CodeNameProjection::getCode)).toList();
                yield new AdministrativeAreaResponseDto<>(codeNameProjections);
            }

            case LOCALGOVERNMENT -> {
                if (!notNullEmpty(partOf)) {
                    throw new MissingDataException("Missing Administrative Area partOf");
                }

                List<CodeNameProjection> codeNameProjections = dbLocalGovernmentService.dbCodeNameList(queryMap).parallelStream()
                        .sorted(Comparator.comparing(CodeNameProjection::getCode)).toList();
                yield new AdministrativeAreaResponseDto<>(codeNameProjections);
            }

            case COUNTY -> {
                if (!notNullEmpty(partOf)) {
                    throw new MissingDataException("Missing Administrative Area partOf");
                }

                List<CodeNameProjection> codeNameProjections = dbCountyService.dbCodeNameList(queryMap).parallelStream()
                        .sorted(Comparator.comparing(CodeNameProjection::getCode)).toList();
                yield new AdministrativeAreaResponseDto<>(codeNameProjections);
            }

            case SUBCOUNTY -> {
                if (!notNullEmpty(partOf)) {
                    throw new MissingDataException("Missing Administrative Area partOf");
                }

                List<CodeNameProjection> codeNameProjections = dbSubCountyService.dbCodeNameList(queryMap).parallelStream()
                        .sorted(Comparator.comparing(CodeNameProjection::getCode)).toList();
                yield new AdministrativeAreaResponseDto<>(codeNameProjections);
            }

            case PARISH -> {
                if (!notNullEmpty(partOf)) {
                    throw new MissingDataException("Missing Administrative Area partOf");
                }

                List<CodeNameProjection> codeNameProjections = dbParishService.dbCodeNameList(queryMap).parallelStream()
                        .sorted(Comparator.comparing(CodeNameProjection::getCode)).toList();
                yield new AdministrativeAreaResponseDto<>(codeNameProjections);
            }
        };

    }

    @Override
    public AdministrativeAreaResponseDto<List<?>> searchList(Map<String, String> queryMap) {
        String type = queryMap.get("type");
        String partOf = queryMap.get("partOf");


        Optional<AdministrativeAreaType> optionalAdministrativeAreaType = AdministrativeAreaType.administrativeAreaTypeStr(type);
        if (optionalAdministrativeAreaType.isEmpty()) {
            throw new MissingDataException("Missing Administrative Area Type");
        }

        AdministrativeAreaType administrativeAreaType = optionalAdministrativeAreaType.get();

       return switch (administrativeAreaType){
            case REGION -> {
                List<RegionDto> regionDtos = dbRegionService.dbList().parallelStream().map(region -> convertRegionDto(region)).sorted(Comparator.comparing(RegionDto::getCode)).toList();
                yield new AdministrativeAreaResponseDto<>(regionDtos);
            }
            case SUBREGION ->{

                List<SubRegionDto> subRegionDtos;
                if (notNullEmpty(partOf)) {
                    subRegionDtos = dbSubRegionService.dbByRegionCode(partOf).parallelStream().map(subRegion -> convertSubRegionDto(subRegion)).sorted(Comparator.comparing(SubRegionDto::getCode)).toList();
                }else{
                    subRegionDtos = dbSubRegionService.dbList()
                            .parallelStream().map(subRegion -> convertSubRegionDto(subRegion)).sorted(Comparator.comparing(SubRegionDto::getCode)).toList();
                }
                yield new AdministrativeAreaResponseDto<>(subRegionDtos);

            }
            case LOCALGOVERNMENT -> {
                List<LocalGovernmentDto> localGovernmentDtos;
                if (notNullEmpty(partOf)) {
                    localGovernmentDtos = dbLocalGovernmentService.dbBySubRegionCode(partOf).parallelStream().map(localGovernment -> convertLocalGovernmentDto(localGovernment)).sorted(Comparator.comparing(LocalGovernmentDto::getCode)).toList();
                }else{
                    localGovernmentDtos = dbLocalGovernmentService.dbList()
                            .parallelStream().map(localGovernment -> convertLocalGovernmentDto(localGovernment)).sorted(Comparator.comparing(LocalGovernmentDto::getCode)).toList();
                }
                yield new AdministrativeAreaResponseDto<>(localGovernmentDtos);
            }
            case COUNTY -> {
                List<CountyDto> countyDtos;
                if (notNullEmpty(partOf)) {
                    countyDtos = dbCountyService.dbAllByLocalGovernmentCode(partOf).parallelStream().map(county -> convertCountyDto(county)).sorted(Comparator.comparing(CountyDto::getCode)).toList();
                }else{
                    countyDtos = dbCountyService.dbList()
                            .parallelStream().map(county -> convertCountyDto(county)).sorted(Comparator.comparing(CountyDto::getCode)).toList();
                }
                yield new AdministrativeAreaResponseDto<>(countyDtos);
            }
            case SUBCOUNTY -> {
                List<SubCountyDto> subCountyDtos;
                if (notNullEmpty(partOf)) {
                    subCountyDtos = dbSubCountyService.dbByCountyCode(partOf).parallelStream().map(subCounty -> convertSubCountyDto(subCounty)).sorted(Comparator.comparing(SubCountyDto::getCode)).toList();
                }else{
                    subCountyDtos = dbSubCountyService.dbList()
                            .parallelStream().map(subCounty -> convertSubCountyDto(subCounty)).sorted(Comparator.comparing(SubCountyDto::getCode)).toList();
                }
                yield new AdministrativeAreaResponseDto<>(subCountyDtos);
            }
            case PARISH -> {
                List<ParishDto> parishDtos;
                if (notNullEmpty(partOf)) {
                    parishDtos = dbParishService.dbBySubCountyCode(partOf).parallelStream().map(parish -> convertParishDto(parish)).sorted(Comparator.comparing(ParishDto::getCode)).toList();
                }else{
                    parishDtos = dbParishService.dbList()
                            .parallelStream().map(parish -> convertParishDto(parish)).sorted(Comparator.comparing(ParishDto::getCode)).toList();
                }
                yield new AdministrativeAreaResponseDto<>(parishDtos);
            }
        };

    }

    private ParishDto convertParishDto(Parish parish) {
        ParishDto dto = new ParishDto();
        dto.setCode(parish.getCode());
        dto.setName(parish.getName());
        dto.setLatitude(parish.getLatitude() != null ? String.valueOf(parish.getLatitude()) : "");
        dto.setLongitude(parish.getLongitude() != null ? String.valueOf(parish.getLongitude()) : "");

        dto.setSubCounty(convertSubCountyDto(parish.getSubCounty()));

        return dto;
    }

    private SubCountyDto convertSubCountyDto(SubCounty subCounty) {
        SubCountyDto dto = new SubCountyDto();
        dto.setCode(subCounty.getCode());
        dto.setName(subCounty.getName());
        dto.setLatitude(subCounty.getLatitude() != null ? String.valueOf(subCounty.getLatitude()) : "");
        dto.setLongitude(subCounty.getLongitude() != null ? String.valueOf(subCounty.getLongitude()) : "");

        dto.setCounty(convertCountyDto(subCounty.getCounty()));

        return dto;
    }

    private CountyDto convertCountyDto(County county) {
        CountyDto dto = new CountyDto();
        dto.setCode(county.getCode());
        dto.setName(county.getName());
        dto.setLatitude(county.getLatitude() != null ? String.valueOf(county.getLatitude()) : "");
        dto.setLongitude(county.getLongitude() != null ? String.valueOf(county.getLongitude()) : "");

        dto.setLocalGovernment(convertLocalGovernmentDto(county.getLocalGovernment()));

        return dto;
    }

    private LocalGovernmentDto convertLocalGovernmentDto(LocalGovernment localGovernment) {
        LocalGovernmentDto dto = new LocalGovernmentDto();
        dto.setCode(localGovernment.getCode());
        dto.setName(localGovernment.getName());
        dto.setLatitude(localGovernment.getLatitude() != null ? String.valueOf(localGovernment.getLatitude()) : "");
        dto.setLongitude(localGovernment.getLongitude() != null ? String.valueOf(localGovernment.getLongitude()) : "");

        dto.setSubRegion(convertSubRegionDto(localGovernment.getSubRegion()));

        return dto;
    }

    private static SubRegionDto convertSubRegionDto(SubRegion subRegion) {
        SubRegionDto dto = new SubRegionDto();
        dto.setCode(subRegion.getCode());
        dto.setName(subRegion.getName());
        dto.setLatitude(subRegion.getLatitude() != null ? String.valueOf(subRegion.getLatitude()) : "");
        dto.setLongitude(subRegion.getLongitude() != null ? String.valueOf(subRegion.getLongitude()) : "");

        dto.setRegion(convertRegionDto(subRegion.getRegion()));

        return dto;
    }

    private static RegionDto convertRegionDto(Region region) {
        RegionDto dto = new RegionDto();
        dto.setCode(region.getCode());
        dto.setName(region.getName());
        dto.setLongitude(region.getLongitude() !=null ? String.valueOf(region.getLongitude()) : "");
        dto.setLatitude(region.getLatitude() !=null ? String.valueOf(region.getLatitude()) : "");
        return dto;
    }


//    private final AdministrativeAreaRepository administrativeAreaRepository;
//
//
//    @Override
//    public ResponseEntity<AdministrativeAreaResponseDto<String>> newOne(AdministrativeAreaDtos.NewAdministrativeAreaDto dto) {
//        Optional<AdministrativeAreaType> optionalAdministrativeAreaType = AdministrativeAreaType.administrativeAreaTypeStr(dto.getAdministrativeAreaType());
//        if (optionalAdministrativeAreaType.isEmpty()) {
//            throw new MissingDataException("Missing Administrative Area Type");
//        }
//        AdministrativeAreaType administrativeAreaType = optionalAdministrativeAreaType.get();
//        Optional<AdministrativeArea> optionalAdministrativeArea = dbByName_Type_PartOf(dto.getName(), administrativeAreaType, dto.getPartOf());
//
//        if (optionalAdministrativeArea.isEmpty()) {
//            return new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success" , "Administrative area already exits") , HttpStatus.ALREADY_REPORTED);
//        }
//
//
//        AdministrativeArea administrativeArea = new AdministrativeArea();
//        administrativeArea.setName(dto.getName());
//        administrativeArea.setPartOf(dto.getPartOf());
//        administrativeArea.setLatitude(dto.getLatitude() != null ? Double.valueOf(dto.getLatitude()) : null);
//        administrativeArea.setLongitude(dto.getLongitude() !=null ? Double.valueOf(dto.getLongitude()) : null);
//
//
////        administrativeArea.setAdministrativeAreaType(administrativeAreaType);
//
//        // generate new code
//        String code = generateCode();
//        administrativeArea.setCode(code);
//
//        dbNew(administrativeArea);
//        return new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success" , "success") , HttpStatus.CREATED);
//    }
//
//    @Override
//    public ResponseEntity<AdministrativeAreaResponseDto<String>> newList(List<AdministrativeAreaDtos.NewAdministrativeAreaDto> dtos) {
//
//        // check if all have type
//        if (dtos.parallelStream().anyMatch(dto -> AdministrativeAreaType.administrativeAreaTypeStr(dto.getAdministrativeAreaType()).isEmpty())) {
//            throw new MissingDataException("Found Administrative Area without Type");
//        }
//
//        // exclude existing ones
//        List<AdministrativeArea> administrativeAreaList = dtos.parallelStream().filter(dto -> {
//            AdministrativeAreaType administrativeAreaType = AdministrativeAreaType.administrativeAreaTypeStr(dto.getAdministrativeAreaType()).get();
//            return dbByName_Type_PartOf(dto.getName(), administrativeAreaType, dto.getPartOf()).isEmpty();
//        }).map(dto -> {
//            AdministrativeArea administrativeArea = new AdministrativeArea();
//            administrativeArea.setName(dto.getName());
//            administrativeArea.setPartOf(dto.getPartOf());
//            administrativeArea.setLatitude(dto.getLatitude() != null ? Double.valueOf(dto.getLatitude()) : null);
//            administrativeArea.setLongitude(dto.getLongitude() != null ? Double.valueOf(dto.getLongitude()) : null);
//
//            AdministrativeAreaType administrativeAreaType = AdministrativeAreaType.administrativeAreaTypeStr(dto.getAdministrativeAreaType()).get();
////            administrativeArea.setAdministrativeAreaType(administrativeAreaType);
//
//            // generate new code
//            String code = generateCode();
//            administrativeArea.setCode(code);
//
//            return administrativeArea;
//        }).toList();
//
//
//        dbNew(administrativeAreaList);
//        return new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success" , "successfully added "+administrativeAreaList.size()+" administrative areas") , HttpStatus.CREATED);
//    }
//
//
//    @Override
//    public AdministrativeAreaResponseDto<AdministrativeAreaDtos.AdministrativeAreaDto> filterOne(Map<String, String> queryMap) {
//        String name = queryMap.get("name");
//        String code = queryMap.get("code");
//
//        if (name != null){
//            AdministrativeArea administrativeArea = dbByName(name).orElseThrow(() -> new NotFoundException("Administrative Area not found"));
//
//            return new AdministrativeAreaResponseDto<>(convertToDto1(administrativeArea));
//        }
//
//        if (code != null){
//            AdministrativeArea administrativeArea = dbByCode(code).orElseThrow(() -> new NotFoundException("Administrative Area not found"));
//            return new AdministrativeAreaResponseDto<>(convertToDto1(administrativeArea));
//        }
//        return null;
//    }
//
//    @Override
//    public AdministrativeAreaResponseDto<List<AdministrativeAreaDtos.AdministrativeAreaDto>> filterList(Map<String, String> queryMap) {
//        String type = queryMap.get("type");
//        String partOf = queryMap.get("partOf");
//        String name = queryMap.get("name");
//        String code = queryMap.get("code");
//
//
//        if (partOf != null) {
//            List<AdministrativeAreaDtos.AdministrativeAreaDto> administrativeAreaDtos = dbAllByPartOf(partOf).parallelStream().map(this::convertToDto1).sorted(Comparator.comparing(AdministrativeAreaDtos.AdministrativeAreaDto::getName)).toList();
//            return new AdministrativeAreaResponseDto<>(administrativeAreaDtos);
//        }
//
//        if (type != null) {
//            AdministrativeAreaType administrativeAreaType = AdministrativeAreaType.administrativeAreaTypeStr(type).orElseThrow(() -> new InvalidException("Invalid type " + type));
//            List<AdministrativeAreaDtos.AdministrativeAreaDto> administrativeAreaDtos = dbAllByAdministrativeType(administrativeAreaType).parallelStream().map(this::convertToDto1).sorted(Comparator.comparing(AdministrativeAreaDtos.AdministrativeAreaDto::getName)).toList();
//            return new AdministrativeAreaResponseDto<>(administrativeAreaDtos);
//        }
//
//
//        if (name != null){
//            List<AdministrativeAreaDtos.AdministrativeAreaDto> administrativeAreaDtos = dbAllByNameLike(name).parallelStream().map(this::convertToDto1).sorted(Comparator.comparing(AdministrativeAreaDtos.AdministrativeAreaDto::getName)).toList();
//            return new AdministrativeAreaResponseDto<>(administrativeAreaDtos);
//        }
//
//        if (code != null){
//            List<AdministrativeAreaDtos.AdministrativeAreaDto> administrativeAreaDtos = dbAllByCodeLike(code).parallelStream().map(this::convertToDto1).sorted(Comparator.comparing(AdministrativeAreaDtos.AdministrativeAreaDto::getName)).toList();
//            return new AdministrativeAreaResponseDto<>(administrativeAreaDtos);
//        }
//
//
//        return new AdministrativeAreaResponseDto<>(Collections.emptyList());
//    }
//
//
//
//    private AdministrativeAreaDtos.AdministrativeAreaDto convertToDto1(AdministrativeArea administrativeArea){
//        AdministrativeAreaDtos.AdministrativeAreaDto dto = new AdministrativeAreaDtos.AdministrativeAreaDto();
//        dto.setCode(administrativeArea.getCode());
//        dto.setPartOf(administrativeArea.getPartOf());
//        dto.setName(administrativeArea.getName());
//        dto.setLongitude(administrativeArea.getLongitude() != null ? String.valueOf(administrativeArea.getLongitude()) : null);
//        dto.setLatitude(administrativeArea.getLatitude() != null ? String.valueOf(administrativeArea.getLatitude()) : null);
//        return dto;
//    }
//
//    @Override
//    public String generateCode() {
//        String code;
//        do{
//            code = UUID.randomUUID().toString();
//        }while (administrativeAreaRepository.findByCodeIgnoreCase(code).isPresent());
//        return code;
//    }
//
//    @Override
//    public AdministrativeArea dbNew(AdministrativeArea administrativeArea) {
//        return administrativeAreaRepository.save(administrativeArea);
//    }
//
//    @Override
//    public List<AdministrativeArea> dbNew(List<AdministrativeArea> administrativeAreas) {
//        return administrativeAreaRepository.saveAll(administrativeAreas);
//    }
//
//    @Override
//    public List<AdministrativeArea> dbAllByPartOf(String partOf) {
//        return administrativeAreaRepository.findByPartOf(partOf);
//    }
//
//    @Override
//    public List<AdministrativeArea> dbAllByAdministrativeType(AdministrativeAreaType administrativeAreaType) {
//        return administrativeAreaRepository.findByAdministrativeAreaType(administrativeAreaType);
//    }
//
//    @Override
//    public List<AdministrativeArea> dbAllByCodeLike(String code) {
//        return administrativeAreaRepository.findAllByCodeLikeIgnoreCase(code);
//    }
//
//    @Override
//    public List<AdministrativeArea> dbAllByNameLike(String name) {
//        return administrativeAreaRepository.findAllByNameLikeIgnoreCase(name);
//    }
//
//    @Override
//    public Optional<AdministrativeArea> dbByName_Type_PartOf(String name, AdministrativeAreaType administrativeAreaType, String partOf) {
//        return administrativeAreaRepository.findByNameIgnoreCaseAndAdministrativeAreaTypeAndPartOf(name , administrativeAreaType , partOf);
//    }
//
//    @Override
//    public Optional<AdministrativeArea> dbByCode(String code) {
//        return administrativeAreaRepository.findByCodeIgnoreCase(code);
//    }
//
//    @Override
//    public Optional<AdministrativeArea> dbByName(String name) {
//        return administrativeAreaRepository.findByNameIgnoreCase(name);
//    }
}
