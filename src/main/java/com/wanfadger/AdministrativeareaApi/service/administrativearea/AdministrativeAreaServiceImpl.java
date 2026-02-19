package com.wanfadger.AdministrativeareaApi.service.administrativearea;

import com.wanfadger.AdministrativeareaApi.areaexceptions.MissingDataException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.NotFoundException;
import com.wanfadger.AdministrativeareaApi.dto.*;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.entity.*;
import com.wanfadger.AdministrativeareaApi.repository.*;
import com.wanfadger.AdministrativeareaApi.service.county.CountyService;
import com.wanfadger.AdministrativeareaApi.service.localgovernment.LocalGovernmentService;
import com.wanfadger.AdministrativeareaApi.service.parish.ParishService;
import com.wanfadger.AdministrativeareaApi.service.region.RegionService;
import com.wanfadger.AdministrativeareaApi.service.subRegion.SubRegionService;
import com.wanfadger.AdministrativeareaApi.service.subcounty.SubCountyService;

import jakarta.validation.constraints.NotBlank;

import com.wanfadger.AdministrativeareaApi.repository.specification.GenericSpecification;
import com.wanfadger.AdministrativeareaApi.enums.MatchType;
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
                RegionDTO dto = regionService.findByCode(code).getData();
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

                yield new ResponseDTO<>(dtos.stream()
                        .map(d -> new CodeNameDTO(d.getCode(), d.getName()))
                        .collect(Collectors.toList()));
            }
            case SUBREGION -> {
                if (!notNullEmpty(partOf))
                    throw new MissingDataException("Missing Administrative Area partOf");
                List<SubRegionDTO> dtos = new ArrayList<>();
                int page = 1;
                int size = 100;
                PaginatedResponseDTO<SubRegionDTO> response = subRegionService
                        .search(Map.of("region.code", partOf, "page", String.valueOf(page), "size",
                                String.valueOf(size)));

                while (response.getTotalElements() > 0) {
                    dtos.addAll(response.getData());
                    if (!response.isHasNext())
                        break;
                    page++;
                    response = subRegionService.search(Map.of("region.code", partOf, "page", String.valueOf(page),
                            "size", String.valueOf(size)));
                }

                yield new ResponseDTO<>(dtos.stream()
                        .map(d -> new CodeNameDTO(d.getCode(), d.getName()))
                        .collect(Collectors.toList()));
            }
            case LOCALGOVERNMENT -> {
                if (!notNullEmpty(partOf))
                    throw new MissingDataException("Missing Administrative Area partOf");
                List<LocalGovernmentDTO> dtos = new ArrayList<>();
                int page = 1;
                int size = 100;
                PaginatedResponseDTO<LocalGovernmentDTO> response = localGovernmentService
                        .search(Map.of("subRegion.code", partOf, "page", String.valueOf(page), "size",
                                String.valueOf(size)));

                while (response.getTotalElements() > 0) {
                    dtos.addAll(response.getData());
                    if (!response.isHasNext())
                        break;
                    page++;
                    response = localGovernmentService.search(Map.of("subRegion.code", partOf, "page",
                            String.valueOf(page), "size", String.valueOf(size)));
                }
                yield new ResponseDTO<>(dtos.stream()
                        .map(d -> new CodeNameDTO(d.getCode(), d.getName()))
                        .collect(Collectors.toList()));
            }
            case COUNTY -> {
                if (!notNullEmpty(partOf))
                    throw new MissingDataException("Missing Administrative Area partOf");
                List<CountyDTO> dtos = new ArrayList<>();
                int page = 1;
                int size = 100;
                PaginatedResponseDTO<CountyDTO> response = countyService
                        .search(Map.of("localGovernment.code", partOf, "page", String.valueOf(page), "size",
                                String.valueOf(size)));

                while (response.getTotalElements() > 0) {
                    dtos.addAll(response.getData());
                    if (!response.isHasNext())
                        break;
                    page++;
                    response = countyService.search(Map.of("localGovernment.code", partOf, "page",
                            String.valueOf(page), "size", String.valueOf(size)));
                }
                yield new ResponseDTO<>(dtos.stream()
                        .map(d -> new CodeNameDTO(d.getCode(), d.getName()))
                        .collect(Collectors.toList()));
            }
            case SUBCOUNTY -> {
                if (!notNullEmpty(partOf))
                    throw new MissingDataException("Missing Administrative Area partOf");
                List<SubCountyDTO> dtos = new ArrayList<>();
                int page = 1;
                int size = 100;
                PaginatedResponseDTO<SubCountyDTO> response = subCountyService
                        .search(Map.of("county.code", partOf, "page", String.valueOf(page), "size",
                                String.valueOf(size)));

                while (response.getTotalElements() > 0) {
                    dtos.addAll(response.getData());
                    if (!response.isHasNext())
                        break;
                    page++;
                    response = subCountyService.search(Map.of("county.code", partOf, "page", String.valueOf(page),
                            "size", String.valueOf(size)));
                }
                yield new ResponseDTO<>(dtos.stream()
                        .map(d -> new CodeNameDTO(d.getCode(), d.getName()))
                        .collect(Collectors.toList()));
            }
            case PARISH -> {
                if (!notNullEmpty(partOf))
                    throw new MissingDataException("Missing Administrative Area partOf");
                List<ParishDTO> dtos = new ArrayList<>();
                int page = 1;
                int size = 100;
                PaginatedResponseDTO<ParishDTO> response = parishService
                        .search(Map.of("subCounty.code", partOf, "page", String.valueOf(page), "size",
                                String.valueOf(size)));

                while (response.getTotalElements() > 0) {
                    dtos.addAll(response.getData());
                    if (!response.isHasNext())
                        break;
                    page++;
                    response = parishService.search(Map.of("subCounty.code", partOf, "page", String.valueOf(page),
                            "size", String.valueOf(size)));
                }
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
                List<String> subRegionCodes = subRegionRepository
                        .findAll(new GenericSpecification<>(
                                new SearchCriteria("region.code", partOfCode, MatchType.EQUALS)))
                        .parallelStream().map(SubRegion::getCode).distinct().toList();
                List<String> lgCodes = localGovernmentRepository
                        .findAll(new GenericSpecification<>(
                                new SearchCriteria("subRegion.code", subRegionCodes, MatchType.IN)))
                        .parallelStream().map(LocalGovernment::getCode).distinct().toList();
                List<String> countyCodes = countyRepository
                        .findAll(new GenericSpecification<>(
                                new SearchCriteria("localGovernment.code", lgCodes, MatchType.IN)))
                        .parallelStream().map(County::getCode).distinct().toList();
                List<String> subCounties = subCountyRepository
                        .findAll(new GenericSpecification<>(
                                new SearchCriteria("county.code", countyCodes, MatchType.IN)))
                        .parallelStream().map(SubCounty::getCode).distinct().toList();
                List<CodeNameDTO> codeNameDtoList = parishRepository
                        .findAll(new GenericSpecification<>(
                                new SearchCriteria("subCounty.code", subCounties, MatchType.IN)))
                        .parallelStream().map(parish -> new CodeNameDTO(parish.getCode(), parish.getName()))
                        .distinct().toList();
                return new ResponseDTO<>(codeNameDtoList);
            }
            case SUBREGION -> {
                List<String> lgCodes = localGovernmentRepository
                        .findAll(new GenericSpecification<>(
                                new SearchCriteria("subRegion.code", partOfCode, MatchType.EQUALS)))
                        .parallelStream().map(LocalGovernment::getCode).distinct().toList();
                List<String> countyCodes = countyRepository
                        .findAll(new GenericSpecification<>(
                                new SearchCriteria("localGovernment.code", lgCodes, MatchType.IN)))
                        .parallelStream().map(County::getCode).distinct().toList();
                List<String> subCounties = subCountyRepository
                        .findAll(new GenericSpecification<>(
                                new SearchCriteria("county.code", countyCodes, MatchType.IN)))
                        .parallelStream().map(SubCounty::getCode).distinct().toList();
                List<CodeNameDTO> codeNameDtoList = parishRepository
                        .findAll(new GenericSpecification<>(
                                new SearchCriteria("subCounty.code", subCounties, MatchType.IN)))
                        .parallelStream().map(parish -> new CodeNameDTO(parish.getCode(), parish.getName()))
                        .sorted(Comparator.comparing(CodeNameDTO::getCode)).toList();
                return new ResponseDTO<>(codeNameDtoList);
            }
            case LOCALGOVERNMENT -> {
                List<String> countyCodes = countyRepository
                        .findAll(new GenericSpecification<>(
                                new SearchCriteria("localGovernment.code", partOfCode, MatchType.EQUALS)))
                        .parallelStream().map(County::getCode).distinct().toList();
                List<String> subCountyCodes = subCountyRepository
                        .findAll(new GenericSpecification<>(
                                new SearchCriteria("county.code", countyCodes, MatchType.IN)))
                        .parallelStream().map(SubCounty::getCode).distinct().toList();
                List<CodeNameDTO> codeNameDtoList = parishRepository
                        .findAll(new GenericSpecification<>(
                                new SearchCriteria("subCounty.code", subCountyCodes, MatchType.IN)))
                        .parallelStream().map(parish -> new CodeNameDTO(parish.getCode(), parish.getName()))
                        .sorted(Comparator.comparing(CodeNameDTO::getCode)).toList();
                return new ResponseDTO<>(codeNameDtoList);
            }
            case COUNTY -> {
                List<String> subCountyCodes = subCountyRepository
                        .findAll(new GenericSpecification<>(
                                new SearchCriteria("county.code", partOfCode, MatchType.EQUALS)))
                        .parallelStream().map(SubCounty::getCode).distinct().toList();
                List<CodeNameDTO> codeNameDtoList = parishRepository
                        .findAll(new GenericSpecification<>(
                                new SearchCriteria("subCounty.code", subCountyCodes, MatchType.IN)))
                        .parallelStream().map(parish -> new CodeNameDTO(parish.getCode(), parish.getName()))
                        .sorted(Comparator.comparing(CodeNameDTO::getCode)).toList();
                return new ResponseDTO<>(codeNameDtoList);
            }
            case SUBCOUNTY -> {
                List<CodeNameDTO> codeNameDtoList = parishRepository
                        .findAll(new GenericSpecification<>(
                                new SearchCriteria("subCounty.code", partOfCode, MatchType.EQUALS)))
                        .parallelStream().map(parish -> new CodeNameDTO(parish.getCode(), parish.getName()))
                        .sorted(Comparator.comparing(CodeNameDTO::getCode)).toList();
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
        if (!notNullEmpty(typeStr))
            throw new MissingDataException("Missing Administrative Area Type");

        AdministrativeAreaType type = AdministrativeAreaType.fromStr(typeStr)
                .orElseThrow(() -> new MissingDataException("Missing Administrative Area Type"));

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
    public ResponseDTO<?> searchOne(Map<String, String> queryMap) {
        String typeStr = queryMap.get("type");
        String code = queryMap.get("code");

        if (!notNullEmpty(typeStr))
            throw new MissingDataException("Missing Administrative Area Type");
        if (!notNullEmpty(code))
            throw new MissingDataException("Missing Administrative Area Code");

        AdministrativeAreaType type = AdministrativeAreaType.fromStr(typeStr)
                .orElseThrow(() -> new MissingDataException("Missing Administrative Area Type"));

        // Use the paginated search with a code filter
        Map<String, String> searchMap = new HashMap<>(queryMap);
        searchMap.put("code", code);

        PaginatedResponseDTO<?> response = (PaginatedResponseDTO<?>) switch (type) {
            case REGION -> regionService.search(searchMap);
            case SUBREGION -> subRegionService.search(searchMap);
            case LOCALGOVERNMENT -> localGovernmentService.search(searchMap);
            case COUNTY -> countyService.search(searchMap);
            case SUBCOUNTY -> subCountyService.search(searchMap);
            case PARISH -> parishService.search(searchMap);
        };

        if (response.getData() == null || response.getData().isEmpty()) {
            throw new NotFoundException(type.name() + " not found with code: " + code);
        }

        return new ResponseDTO<>(response.getData().get(0));
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
}
