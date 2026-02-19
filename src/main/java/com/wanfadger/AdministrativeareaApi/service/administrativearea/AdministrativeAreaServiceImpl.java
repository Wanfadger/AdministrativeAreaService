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
    public ResponseDTO<String> createOne(@NotBlank String type, NewAdministrativeAreaDTO dto) {
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
    public ResponseDTO<List<AdministrativeAreaDTO>> filter(Map<String, String> queryMap) {
        String typeStr = queryMap.get("type");
        String selected = queryMap.get("selected");

        if (!notNullEmpty(typeStr))
            throw new MissingDataException("Missing Administrative Area Type");

        AdministrativeAreaType type = AdministrativeAreaType.fromStr(typeStr)
                .orElseThrow(() -> new MissingDataException("Missing or Unknown Administrative Area Type"));

        if (notNullEmpty(selected)) {
            return switch (type) {
                case REGION -> {
                    List<SubRegionDTO> dtos = new ArrayList<>();
                    int page = 1;
                    int size = 100;
                    PaginatedResponseDTO<SubRegionDTO> response = subRegionService
                            .search(Map.of("region.code", selected, "page", String.valueOf(page), "size",
                                    String.valueOf(size)));

                    while (response.getTotalElements() > 0) {
                        dtos.addAll(response.getData());
                        if (!response.isHasNext())
                            break;
                        page++;
                        response = subRegionService.search(Map.of("region.code", selected, "page", String.valueOf(page),
                                "size", String.valueOf(size)));
                    }

                    yield new ResponseDTO<>(dtos.stream()
                            .map(d -> new AdministrativeAreaDTO(d.getCode(), d.getName(), d.getDescription(),
                                    d.getLatitude(), d.getLongitude()))
                            .collect(Collectors.toList()));
                }
                case SUBREGION -> {
                    List<LocalGovernmentDTO> dtos = new ArrayList<>();
                    int page = 1;
                    int size = 100;
                    PaginatedResponseDTO<LocalGovernmentDTO> response = localGovernmentService
                            .search(Map.of("subRegion.code", selected, "page", String.valueOf(page), "size",
                                    String.valueOf(size)));

                    while (response.getTotalElements() > 0) {
                        dtos.addAll(response.getData());
                        if (!response.isHasNext())
                            break;
                        page++;
                        response = localGovernmentService.search(Map.of("subRegion.code", selected, "page",
                                String.valueOf(page), "size", String.valueOf(size)));
                    }
                    yield new ResponseDTO<>(dtos.stream()
                            .map(d -> new AdministrativeAreaDTO(d.getCode(), d.getName(), d.getDescription(),
                                    d.getLatitude(), d.getLongitude()))
                            .collect(Collectors.toList()));
                }
                case LOCALGOVERNMENT -> {
                    List<CountyDTO> dtos = new ArrayList<>();
                    int page = 1;
                    int size = 100;
                    PaginatedResponseDTO<CountyDTO> response = countyService
                            .search(Map.of("localGovernment.code", selected, "page", String.valueOf(page), "size",
                                    String.valueOf(size)));

                    while (response.getTotalElements() > 0) {
                        dtos.addAll(response.getData());
                        if (!response.isHasNext())
                            break;
                        page++;
                        response = countyService.search(Map.of("localGovernment.code", selected, "page",
                                String.valueOf(page), "size", String.valueOf(size)));
                    }
                    yield new ResponseDTO<>(dtos.stream()
                            .map(d -> new AdministrativeAreaDTO(d.getCode(), d.getName(), d.getDescription(),
                                    d.getLatitude(), d.getLongitude()))
                            .collect(Collectors.toList()));
                }
                case COUNTY -> {
                    List<SubCountyDTO> dtos = new ArrayList<>();
                    int page = 1;
                    int size = 100;
                    PaginatedResponseDTO<SubCountyDTO> response = subCountyService
                            .search(Map.of("county.code", selected, "page", String.valueOf(page), "size",
                                    String.valueOf(size)));

                    while (response.getTotalElements() > 0) {
                        dtos.addAll(response.getData());
                        if (!response.isHasNext())
                            break;
                        page++;
                        response = subCountyService.search(Map.of("county.code", selected, "page", String.valueOf(page),
                                "size", String.valueOf(size)));
                    }
                    yield new ResponseDTO<>(dtos.stream()
                            .map(d -> new AdministrativeAreaDTO(d.getCode(), d.getName(), d.getDescription(),
                                    d.getLatitude(), d.getLongitude()))
                            .collect(Collectors.toList()));
                }
                case SUBCOUNTY -> {
                    List<ParishDTO> dtos = new ArrayList<>();
                    int page = 1;
                    int size = 100;
                    PaginatedResponseDTO<ParishDTO> response = parishService
                            .search(Map.of("subCounty.code", selected, "page", String.valueOf(page), "size",
                                    String.valueOf(size)));

                    while (response.getTotalElements() > 0) {
                        dtos.addAll(response.getData());
                        if (!response.isHasNext())
                            break;
                        page++;
                        response = parishService.search(Map.of("subCounty.code", selected, "page", String.valueOf(page),
                                "size", String.valueOf(size)));
                    }
                    yield new ResponseDTO<>(dtos.stream()
                            .map(d -> new AdministrativeAreaDTO(d.getCode(), d.getName(), d.getDescription(),
                                    d.getLatitude(), d.getLongitude()))
                            .collect(Collectors.toList()));
                }
                case PARISH -> {
                    yield new ResponseDTO<>(Collections.emptyList());
                }
            };
        } else {
            // if nothing is selected , then return regions
            List<RegionDTO> dtos = new ArrayList<>();
            int page = 1;
            int size = 100;
            PaginatedResponseDTO<RegionDTO> response = regionService
                    .search(Map.of("page", String.valueOf(page), "size", String.valueOf(size)));

            while (response.getTotalElements() > 0) {
                dtos.addAll(response.getData());
                page++;
                response = regionService.search(Map.of("page", String.valueOf(page), "size", String.valueOf(size)));
            }

            return new ResponseDTO<>(dtos.stream()
                    .map(d -> new AdministrativeAreaDTO(d.getCode(), d.getName(), d.getDescription(), d.getLatitude(),
                            d.getLongitude()))
                    .collect(Collectors.toList()));
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
