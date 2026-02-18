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
import com.wanfadger.AdministrativeareaApi.repository.specification.GenericSpecification;
import com.wanfadger.AdministrativeareaApi.enums.MatchType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    // Repositories needed for cross-cutting logic (getParishByPartOf and
    // advancedSearch)
    private final RegionRepository regionRepository;
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

    @Override
    public ResponseDTO<?> advancedSearch(Map<String, String> queryMap) {
        String typeStr = queryMap.get("type");
        if (!notNullEmpty(typeStr))
            throw new MissingDataException("Missing Administrative Area Type");

        AdministrativeAreaType type = AdministrativeAreaType.fromStr(typeStr)
                .orElseThrow(() -> new MissingDataException("Invalid Administrative Area Type"));

        // 1. Convert to a mutable map to remove reserved keys
        Map<String, String> filters = new HashMap<>(queryMap);

        // 2. Extract Pagination & Sorting
        int page = Optional.ofNullable(filters.remove("page")).map(Integer::parseInt).orElse(1);
        int size = Optional.ofNullable(filters.remove("size")).map(Integer::parseInt).orElse(10);
        String sortBy = Optional.ofNullable(filters.remove("sortBy")).orElse("id");
        String sortDirection = Optional.ofNullable(filters.remove("sortDirection")).orElse("ASC");
        filters.remove("type"); // Remove type as it's used for routing

        page = page <= 0 ? 0 : page - 1;

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(sortDirection.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy));

        // 3. Build Specification
        Specification<?> spec = buildSpecification(filters);

        // 4. Execute Search based on type
        return executeSearch(type, spec, pageable);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Specification<?> buildSpecification(Map<String, String> filters) {
        Specification spec = Specification.where(null);

        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String fullKey = entry.getKey();
            String value = entry.getValue();

            // Determine Operator (Default: EQUALS)
            String key = fullKey;
            MatchType matchType = MatchType.EQUALS;

            if (fullKey.contains(":")) {
                String[] parts = fullKey.split(":", 2);
                key = parts[0];
                try {
                    matchType = MatchType.valueOf(parts[1].toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid MatchType: {}, defaulting to EQUALS", parts[1]);
                }
            }

            spec = spec.and(new GenericSpecification<>(new SearchCriteria(key, value, matchType)));
        }
        return spec;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private ResponseDTO<?> executeSearch(AdministrativeAreaType type, Specification spec, Pageable pageable) {
        Page<?> resultPage = switch (type) {
            case REGION -> regionRepository.findAll(spec, pageable);
            case SUBREGION -> subRegionRepository.findAll(spec, pageable);
            case LOCALGOVERNMENT -> localGovernmentRepository.findAll(spec, pageable);
            case COUNTY -> countyRepository.findAll(spec, pageable);
            case SUBCOUNTY -> subCountyRepository.findAll(spec, pageable);
            case PARISH -> parishRepository.findAll(spec, pageable);
        };

        // For simplicity, returning the content list as per previous search pattern,
        // but ideally should return a paginated DTO.
        // Given existing response types, I'll return the list of data.
        return new ResponseDTO<>(resultPage.getContent());
    }
}
