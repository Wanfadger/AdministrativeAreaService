package com.wanfadger.AdministrativeareaApi.service.administrativearea;

import com.wanfadger.AdministrativeareaApi.areaexceptions.MissingDataException;
import com.wanfadger.AdministrativeareaApi.dto.*;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.entity.*;
import com.wanfadger.AdministrativeareaApi.service.county.CountyService;
import com.wanfadger.AdministrativeareaApi.service.localgovernment.LocalGovernmentService;
import com.wanfadger.AdministrativeareaApi.service.parish.ParishService;
import com.wanfadger.AdministrativeareaApi.service.region.RegionService;
import com.wanfadger.AdministrativeareaApi.service.subRegion.SubRegionService;
import com.wanfadger.AdministrativeareaApi.service.subcounty.SubCountyService;

import jakarta.validation.constraints.NotBlank;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
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

    private boolean notNullEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    @Override
    public ResponseDTO<String> create(@NotBlank String type, NewAdministrativeAreaDTO dto) {
        AdministrativeAreaType areaType = AdministrativeAreaType.fromStr(type)
                .orElseThrow(() -> new MissingDataException("Missing Administrative Area Type"));

        ResponseDTO<String> response = switch (areaType) {
            case REGION -> regionService.create(dto);
            case SUBREGION -> subRegionService.create(dto);
            case LOCALGOVERNMENT -> localGovernmentService.create(dto);
            case COUNTY -> countyService.create(dto);
            case SUBCOUNTY -> subCountyService.create(dto);
            case PARISH -> parishService.create(dto);
        };

        return response;
    }

    @Override
    public ResponseDTO<String> createList(@NotBlank String type, List<NewAdministrativeAreaDTO> dtos) {
        AdministrativeAreaType areaType = AdministrativeAreaType.fromStr(type)
                .orElseThrow(() -> new MissingDataException("Missing Administrative Area Type"));

        ResponseDTO<String> response = switch (areaType) {
            case REGION -> regionService.createList(dtos);
            case SUBREGION -> subRegionService.createAll(dtos);
            case LOCALGOVERNMENT -> localGovernmentService.createAll(dtos);
            case COUNTY -> countyService.createAll(dtos);
            case SUBCOUNTY -> subCountyService.createAll(dtos);
            case PARISH -> parishService.createAll(dtos);
        };

        return response;
    }

    @Override
    public PaginatedResponseDTO<? extends AdministrativeAreaDTO> filter(Map<String, String> queryMap) {
        String typeStr = queryMap.get("type");
        String selected = queryMap.get("selected");

        if (!notNullEmpty(typeStr))
            throw new MissingDataException("Missing Administrative Area Type");

        AdministrativeAreaType type = AdministrativeAreaType.fromStr(typeStr)
                .orElseThrow(() -> new MissingDataException("Missing or Unknown Administrative Area Type"));

        if (notNullEmpty(selected)) {
            return switch (type) {
                case REGION -> subRegionService.filter(queryMap);// fetches selected region sub regions
                case SUBREGION -> localGovernmentService.filter(queryMap); // fetches selected subregion local
                                                                           // governments
                case LOCALGOVERNMENT -> countyService.filter(queryMap); // fetches selected local government counties
                case COUNTY -> subCountyService.filter(queryMap); // fetches selected county sub counties
                case SUBCOUNTY -> parishService.filter(queryMap); // fetches selected sub county parishes
                default -> regionService.search(queryMap);
            };

        } else {
            return regionService.search(queryMap);
        }
    }

    @Override
    public PaginatedResponseDTO<? extends AdministrativeAreaDTO> search(Map<String, String> queryMap) {
        String typeStr = queryMap.get("type");
        if (!notNullEmpty(typeStr))
            throw new MissingDataException("Missing Administrative Area Type");

        AdministrativeAreaType type = AdministrativeAreaType.fromStr(typeStr)
                .orElseThrow(() -> new MissingDataException("Invalid Administrative Area Type"));

        return switch (type) {
            case REGION -> regionService.search(queryMap);
            case SUBREGION -> subRegionService.search(queryMap);
            case LOCALGOVERNMENT -> localGovernmentService.search(queryMap);
            case COUNTY -> countyService.search(queryMap);
            case SUBCOUNTY -> subCountyService.search(queryMap);
            case PARISH -> parishService.search(queryMap);
        };
    }

    @Override
    public ResponseDTO<?> getByCode(@NotBlank String typeStr, @NotBlank String code) {
        if (!notNullEmpty(typeStr))
            throw new MissingDataException("Missing Administrative Area Type");
        if (!notNullEmpty(code))
            throw new MissingDataException("Missing Administrative Area Code");

        AdministrativeAreaType type = AdministrativeAreaType.fromStr(typeStr)
                .orElseThrow(() -> new MissingDataException("Missing or Unknown Administrative Area Type"));

        ResponseDTO<?> response = (ResponseDTO<?>) switch (type) {
            case REGION -> regionService.findByCode(code);
            case SUBREGION -> subRegionService.findByCode(code);
            case LOCALGOVERNMENT -> localGovernmentService.getByCode(code);
            case COUNTY -> countyService.getByCode(code);
            case SUBCOUNTY -> subCountyService.getByCode(code);
            case PARISH -> parishService.getByCode(code);
        };

        return response;
    }

    @Override
    public ResponseDTO<?> getDetailsByCode(@NotBlank String typeStr, @NotBlank String code) {
        if (!notNullEmpty(typeStr))
            throw new MissingDataException("Missing Administrative Area Type");
        if (!notNullEmpty(code))
            throw new MissingDataException("Missing Administrative Area Code");

        AdministrativeAreaType type = AdministrativeAreaType.fromStr(typeStr)
                .orElseThrow(() -> new MissingDataException("Missing or Unknown Administrative Area Type"));

        ResponseDTO<?> response = (ResponseDTO<?>) switch (type) {
            case REGION -> regionService.findDetailsByCode(code);
            case SUBREGION -> subRegionService.findDetailsByCode(code);
            case LOCALGOVERNMENT -> localGovernmentService.findDetailsByCode(code);
            case COUNTY -> countyService.findDetailsByCode(code);
            case SUBCOUNTY -> subCountyService.findDetailsByCode(code);
            case PARISH -> parishService.getDetailsByCode(code);
        };

        return response;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public ResponseDTO<String> excelJson(List<ExcelJsonDTO> excelJsonDtos) {
        if (excelJsonDtos == null || excelJsonDtos.isEmpty()) {
            return new ResponseDTO<>("No data to upload");
        }

        try {
            CompletableFuture
                    .supplyAsync(() -> regionService.upload(excelJsonDtos).stream()
                            .collect(Collectors.toMap(Region::getName, (r) -> r, (existing, replacement) -> existing)))
                    .thenApplyAsync(regionMap -> subRegionService.upload(excelJsonDtos, regionMap).stream().collect(
                            Collectors.toMap(SubRegion::getName, (r) -> r, (existing, replacement) -> existing)))
                    .thenApplyAsync(subRegionMap -> localGovernmentService.upload(excelJsonDtos, subRegionMap).stream()
                            .collect(Collectors.toMap(LocalGovernment::getName, (r) -> r,
                                    (existing, replacement) -> existing)))
                    .thenApplyAsync(lgMap -> countyService.upload(excelJsonDtos, lgMap).stream()
                            .collect(Collectors.toMap(County::getName, (r) -> r, (existing, replacement) -> existing)))
                    .thenApplyAsync(countyMap -> subCountyService.upload(excelJsonDtos, countyMap).stream().collect(
                            Collectors.toMap(SubCounty::getName, (r) -> r, (existing, replacement) -> existing)))
                    .thenApplyAsync(subCountyMap -> parishService.upload(excelJsonDtos, subCountyMap).stream()
                            .collect(Collectors.toMap(Parish::getName, (r) -> r, (existing, replacement) -> existing)))
                    .join(); // Wait for completion
        } catch (Exception e) {
            log.error("Error processing Excel upload", e);
            throw new RuntimeException("Error processing Excel upload: " + e.getMessage(), e);
        }

        return new ResponseDTO<>("Upload processed successfully");
    }

    @Override
    public ResponseDTO<String> update(String typeStr, UpdateAdministrativeAreaDTO dto) {
        if (!notNullEmpty(typeStr))
            throw new MissingDataException("Missing Administrative Area Type");

        AdministrativeAreaType areaType = AdministrativeAreaType.fromStr(typeStr)
                .orElseThrow(() -> new MissingDataException("Missing or Unknown Administrative Area Type: " + typeStr));

        return switch (areaType) {
            case REGION -> regionService.update(dto);
            case SUBREGION -> subRegionService.update(dto);
            case LOCALGOVERNMENT -> localGovernmentService.update(dto);
            case COUNTY -> countyService.update(dto);
            case SUBCOUNTY -> subCountyService.update(dto);
            case PARISH -> parishService.update(dto);
        };
    }

    @Override
    public ResponseDTO<String> delete(String typeStr, String code) {
        if (!notNullEmpty(typeStr))
            throw new MissingDataException("Missing Administrative Area Type");
        if (!notNullEmpty(code))
            throw new MissingDataException("Missing Administrative Area Code");

        AdministrativeAreaType type = AdministrativeAreaType.fromStr(typeStr)
                .orElseThrow(() -> new MissingDataException("Missing or Unknown Administrative Area Type"));

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
