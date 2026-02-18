package com.wanfadger.AdministrativeareaApi.service.administrativearea;

import com.wanfadger.AdministrativeareaApi.areaexceptions.MissingDataException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.NotFoundException;
import com.wanfadger.AdministrativeareaApi.dto.*;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.entity.*;
import com.wanfadger.AdministrativeareaApi.repository.*;
import com.wanfadger.AdministrativeareaApi.service.county.CountyService;
import com.wanfadger.AdministrativeareaApi.service.localgovernment.LocalGovernmentService;
import com.wanfadger.AdministrativeareaApi.service.parish.ParishService;
import com.wanfadger.AdministrativeareaApi.service.region.RegionService;
import com.wanfadger.AdministrativeareaApi.service.subRegion.SubRegionService;
import com.wanfadger.AdministrativeareaApi.service.subcounty.SubCountyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdministrativeAreaServiceImpl implements AdministrativeAreaService {

    private final RegionService regionService;
    private final SubRegionService subRegionService;
    private final LocalGovernmentService localGovernmentService;
    private final CountyService countyService;
    private final SubCountyService subCountyService;
    private final ParishService parishService;

    // Repositories needed for cross-cutting logic (getParishByPartOf)
    private final SubRegionRepository subRegionRepository;
    private final LocalGovernmentRepository localGovernmentRepository;
    private final CountyRepository countyRepository;
    private final SubCountyRepository subCountyRepository;
    private final ParishRepository parishRepository;

    private boolean notNullEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    @Override
    public ResponseEntity<ResponseDTO<String>> newOne(Map<String, String> queryMap, NewAdministrativeAreaDTO dto) {
        AdministrativeAreaType type = AdministrativeAreaType.fromStr(queryMap.get("type"))
                .orElseThrow(() -> new MissingDataException("Missing Administrative Area Type"));

        ResponseDTO<String> response = switch (type) {
            case REGION -> regionService.create(dto);
            case SUBREGION -> subRegionService.create(dto);
            case LOCALGOVERNMENT -> localGovernmentService.create(dto);
            case COUNTY -> countyService.create(dto);
            case SUBCOUNTY -> subCountyService.create(dto);
            case PARISH -> parishService.create(dto);
        };

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<ResponseDTO<String>> newList(Map<String, String> queryMap,
            List<NewAdministrativeAreaDTO> dtos) {
        AdministrativeAreaType type = AdministrativeAreaType.fromStr(queryMap.get("type"))
                .orElseThrow(() -> new MissingDataException("Missing Administrative Area Type"));

        ResponseDTO<String> response = switch (type) {
            case REGION -> regionService.createAll(dtos);
            case SUBREGION -> subRegionService.createAll(dtos);
            case LOCALGOVERNMENT -> localGovernmentService.createAll(dtos);
            case COUNTY -> countyService.createAll(dtos);
            case SUBCOUNTY -> subCountyService.createAll(dtos);
            case PARISH -> parishService.createAll(dtos);
        };

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Override
    public ResponseDTO<CodeNameDTO> filterOne(Map<String, String> queryMap) {
        String typeStr = queryMap.get("type");
        String code = queryMap.get("code");

        if (!notNullEmpty(typeStr))
            throw new MissingDataException("Missing Administrative Area Type");
        if (!notNullEmpty(code))
            throw new MissingDataException("Missing Administrative Area Code");

        AdministrativeAreaType type = AdministrativeAreaType.fromStr(typeStr)
                .orElseThrow(() -> new MissingDataException("Missing Administrative Area Type"));

        return switch (type) {
            case REGION -> {
                RegionDTO dto = regionService.getByCode(code).getData();
                yield new ResponseDTO<>(new CodeNameDTO(dto.getCode(), dto.getName()));
            }
            case SUBREGION -> {
                SubRegionDTO dto = subRegionService.getByCode(code).getData();
                yield new ResponseDTO<>(new CodeNameDTO(dto.getCode(), dto.getName()));
            }
            case LOCALGOVERNMENT -> {
                LocalGovernmentDTO dto = localGovernmentService.getByCode(code).getData();
                yield new ResponseDTO<>(new CodeNameDTO(dto.getCode(), dto.getName()));
            }
            case COUNTY -> {
                CountyDTO dto = countyService.getByCode(code).getData();
                yield new ResponseDTO<>(new CodeNameDTO(dto.getCode(), dto.getName()));
            }
            case SUBCOUNTY -> {
                SubCountyDTO dto = subCountyService.getByCode(code).getData();
                yield new ResponseDTO<>(new CodeNameDTO(dto.getCode(), dto.getName()));
            }
            case PARISH -> {
                ParishDTO dto = parishService.getByCode(code).getData();
                yield new ResponseDTO<>(new CodeNameDTO(dto.getCode(), dto.getName()));
            }
        };
    }

    @Override
    public ResponseDTO<List<CodeNameDTO>> filterList(Map<String, String> queryMap) {
        String typeStr = queryMap.get("type");
        String partOf = queryMap.get("partOf");

        if (!notNullEmpty(typeStr))
            throw new MissingDataException("Missing Administrative Area Type");

        AdministrativeAreaType type = AdministrativeAreaType.fromStr(typeStr)
                .orElseThrow(() -> new MissingDataException("Missing Administrative Area Type"));

        return switch (type) {
            case REGION -> {
                // Region doesn't usually filter by 'partOf' unless it's just list all
                List<RegionDTO> dtos = regionService.list().getData();
                yield new ResponseDTO<>(dtos.stream()
                        .map(d -> new CodeNameDTO(d.getCode(), d.getName()))
                        .collect(Collectors.toList()));
            }
            case SUBREGION -> {
                if (!notNullEmpty(partOf))
                    throw new MissingDataException("Missing Administrative Area partOf");
                List<SubRegionDTO> dtos = subRegionService.list(partOf).getData();
                yield new ResponseDTO<>(dtos.stream()
                        .map(d -> new CodeNameDTO(d.getCode(), d.getName()))
                        .collect(Collectors.toList()));
            }
            case LOCALGOVERNMENT -> {
                if (!notNullEmpty(partOf))
                    throw new MissingDataException("Missing Administrative Area partOf");
                List<LocalGovernmentDTO> dtos = localGovernmentService.list(partOf).getData();
                yield new ResponseDTO<>(dtos.stream()
                        .map(d -> new CodeNameDTO(d.getCode(), d.getName()))
                        .collect(Collectors.toList()));
            }
            case COUNTY -> {
                if (!notNullEmpty(partOf))
                    throw new MissingDataException("Missing Administrative Area partOf");
                List<CountyDTO> dtos = countyService.list(partOf).getData();
                yield new ResponseDTO<>(dtos.stream()
                        .map(d -> new CodeNameDTO(d.getCode(), d.getName()))
                        .collect(Collectors.toList()));
            }
            case SUBCOUNTY -> {
                if (!notNullEmpty(partOf))
                    throw new MissingDataException("Missing Administrative Area partOf");
                List<SubCountyDTO> dtos = subCountyService.list(partOf).getData();
                yield new ResponseDTO<>(dtos.stream()
                        .map(d -> new CodeNameDTO(d.getCode(), d.getName()))
                        .collect(Collectors.toList()));
            }
            case PARISH -> {
                if (!notNullEmpty(partOf))
                    throw new MissingDataException("Missing Administrative Area partOf");
                List<ParishDTO> dtos = parishService.list(partOf).getData();
                yield new ResponseDTO<>(dtos.stream()
                        .map(d -> new CodeNameDTO(d.getCode(), d.getName()))
                        .collect(Collectors.toList()));
            }
        };
    }

    @Override
    public ResponseDTO<List<CodeNameDTO>> getParishByPartOf(Map<String, String> queryMap) {
        String typeStr = queryMap.get("type");
        String partOfCode = queryMap.get("partOfCode");

        if (!notNullEmpty(typeStr))
            throw new MissingDataException("Missing Administrative Area Type");
        if (!notNullEmpty(partOfCode))
            throw new MissingDataException("Missing Administrative Area partOfCode");

        AdministrativeAreaType type = AdministrativeAreaType.fromStr(typeStr)
                .orElseThrow(() -> new MissingDataException("Missing Administrative Area Type"));

        // Retaining original logic as this is a cross-cutting traversal
        switch (type) {
            case REGION -> {
                List<String> subRegionCodes = subRegionRepository.findAllByRegion_Code(partOfCode).parallelStream()
                        .map(SubRegion::getCode).distinct().toList();
                List<String> lgCodes = localGovernmentRepository.findAllBySubRegionCodes(subRegionCodes)
                        .parallelStream()
                        .map(LocalGovernment::getCode).distinct().toList();
                List<String> countyCodes = countyRepository.findAllByLocalGovernmentCodes(lgCodes).parallelStream()
                        .map(County::getCode).distinct().toList();
                List<String> subCounties = subCountyRepository.findAllByCountyCodes(countyCodes).parallelStream()
                        .map(SubCounty::getCode).distinct().toList();
                List<CodeNameDTO> codeNameDtoList = parishRepository.findAllBySubCountyCodes(subCounties)
                        .parallelStream()
                        .map(parish -> new CodeNameDTO(parish.getCode(), parish.getName()))
                        .distinct()
                        .toList();
                return new ResponseDTO<>(codeNameDtoList);
            }
            case SUBREGION -> {
                List<String> lgCodes = localGovernmentRepository.findAllBySubRegion_Code(partOfCode).parallelStream()
                        .map(LocalGovernment::getCode).distinct().toList();
                List<String> countyCodes = countyRepository.findAllByLocalGovernmentCodes(lgCodes).parallelStream()
                        .map(County::getCode).distinct().toList();
                List<String> subCounties = subCountyRepository.findAllByCountyCodes(countyCodes).parallelStream()
                        .map(SubCounty::getCode).distinct().toList();
                List<CodeNameDTO> codeNameDtoList = parishRepository.findAllBySubCountyCodes(subCounties)
                        .parallelStream()
                        .map(parish -> new CodeNameDTO(parish.getCode(), parish.getName()))
                        .sorted(Comparator.comparing(CodeNameDTO::getCode))
                        .toList();
                return new ResponseDTO<>(codeNameDtoList);
            }
            case LOCALGOVERNMENT -> {
                List<String> countyCodes = countyRepository.findAllByLocalGovernment_Code(partOfCode).parallelStream()
                        .map(County::getCode).distinct().toList();
                List<String> subCountyCodes = subCountyRepository.findAllByCountyCodes(countyCodes).parallelStream()
                        .map(SubCounty::getCode).distinct().toList();
                List<CodeNameDTO> codeNameDtoList = parishRepository.findAllBySubCountyCodes(subCountyCodes)
                        .parallelStream()
                        .map(parish -> new CodeNameDTO(parish.getCode(), parish.getName()))
                        .sorted(Comparator.comparing(CodeNameDTO::getCode))
                        .toList();
                return new ResponseDTO<>(codeNameDtoList);
            }
            case COUNTY -> {
                List<String> subCountyCodes = subCountyRepository.findAllByCounty_Code(partOfCode).parallelStream()
                        .map(SubCounty::getCode).distinct().toList();
                List<CodeNameDTO> codeNameDtoList = parishRepository.findAllBySubCountyCodes(subCountyCodes)
                        .parallelStream()
                        .map(parish -> new CodeNameDTO(parish.getCode(), parish.getName()))
                        .sorted(Comparator.comparing(CodeNameDTO::getCode))
                        .toList();
                return new ResponseDTO<>(codeNameDtoList);
            }
            case SUBCOUNTY -> {
                List<CodeNameDTO> codeNameDtoList = parishRepository.findAllBySubCounty_Code(partOfCode)
                        .parallelStream()
                        .map(parish -> new CodeNameDTO(parish.getCode(), parish.getName()))
                        .sorted(Comparator.comparing(CodeNameDTO::getCode))
                        .toList();
                return new ResponseDTO<>(codeNameDtoList);
            }
            case PARISH -> {
                return new ResponseDTO<>(Collections.emptyList());
            }
            default ->
                throw new MissingDataException("Unsupported Administrative Area Type: " + type);
        }
    }

    @Override
    public ResponseDTO<?> searchList(Map<String, String> queryMap) {
        String typeStr = queryMap.get("type");
        String partOf = queryMap.get("partOf");

        if (!notNullEmpty(typeStr))
            throw new MissingDataException("Missing Administrative Area Type");

        AdministrativeAreaType type = AdministrativeAreaType.fromStr(typeStr)
                .orElseThrow(() -> new MissingDataException("Missing Administrative Area Type"));

        return switch (type) {
            case REGION -> regionService.list(); // Region search logic/list
            case SUBREGION -> subRegionService.list(partOf);
            case LOCALGOVERNMENT -> localGovernmentService.list(partOf);
            case COUNTY -> countyService.list(partOf);
            case SUBCOUNTY -> subCountyService.list(partOf);
            case PARISH -> parishService.list(partOf);
        };
    }

    @Override
    public ResponseDTO<?> searchOne(Map<String, String> queryMap) {
        String typeStr = queryMap.get("type");
        String code = queryMap.get("code");

        if (!notNullEmpty(typeStr))
            throw new MissingDataException("Missing Administrative Area Type");

        AdministrativeAreaType type = AdministrativeAreaType.fromStr(typeStr)
                .orElseThrow(() -> new MissingDataException("Missing Administrative Area Type"));

        // Search one typically is finding by code but returning full DTO
        ResponseDTO<?> response = switch (type) {
            case REGION -> regionService.search(null, code);
            case SUBREGION -> subRegionService.search(null, code);
            case LOCALGOVERNMENT -> localGovernmentService.search(null, code);
            case COUNTY -> countyService.search(null, code);
            case SUBCOUNTY -> subCountyService.search(null, code);
            case PARISH -> parishService.search(null, code);
        };

        List<?> data = (List<?>) response.getData();
        if (data == null || data.isEmpty()) {
            throw new NotFoundException(type.name() + " not found");
        }
        return new ResponseDTO<>(data.get(0));
    }

    @Override
    public ResponseDTO<String> upload(List<AdministrativeAreaExcelDTO> administrativeAreaExcelDtos) {
        // Delegate upload to all services in hierarchical order
        regionService.upload(administrativeAreaExcelDtos);
        subRegionService.upload(administrativeAreaExcelDtos);
        localGovernmentService.upload(administrativeAreaExcelDtos);
        countyService.upload(administrativeAreaExcelDtos);
        subCountyService.upload(administrativeAreaExcelDtos);
        parishService.upload(administrativeAreaExcelDtos);

        return new ResponseDTO<>("Upload processed");
    }

    @Override
    public ResponseDTO<String> updateOne(Map<String, String> queryMap, UpdateAdministrativeAreaDTO dto) {
        String typeStr = queryMap.get("type");
        String code = dto.getCode();

        if (!notNullEmpty(typeStr))
            throw new MissingDataException("Missing Administrative Area Type");

        AdministrativeAreaType type = AdministrativeAreaType.fromStr(typeStr)
                .orElseThrow(() -> new MissingDataException("Missing Administrative Area Type"));

        return switch (type) {
            case REGION -> regionService.update(code, dto);
            case SUBREGION -> subRegionService.update(code, dto);
            case LOCALGOVERNMENT -> localGovernmentService.update(code, dto);
            case COUNTY -> countyService.update(code, dto);
            case SUBCOUNTY -> subCountyService.update(code, dto);
            case PARISH -> parishService.update(code, dto);
        };
    }

    @Override
    public ResponseDTO<String> deleteOne(Map<String, String> queryMap) {
        String typeStr = queryMap.get("type");
        String code = queryMap.get("code");

        if (!notNullEmpty(typeStr))
            throw new MissingDataException("Missing Administrative Area Type");
        if (!notNullEmpty(code))
            throw new MissingDataException("Missing Administrative Area Code");

        AdministrativeAreaType type = AdministrativeAreaType.fromStr(typeStr)
                .orElseThrow(() -> new MissingDataException("Missing Administrative Area Type"));

        return switch (type) {
            case REGION -> regionService.delete(code);
            case SUBREGION -> subRegionService.delete(code);
            case LOCALGOVERNMENT -> localGovernmentService.delete(code);
            case COUNTY -> countyService.delete(code);
            case SUBCOUNTY -> subCountyService.delete(code);
            case PARISH -> parishService.delete(code);
        };
    }
}
