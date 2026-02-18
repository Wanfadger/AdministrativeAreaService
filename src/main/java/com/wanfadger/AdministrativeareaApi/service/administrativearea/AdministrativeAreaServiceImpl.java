package com.wanfadger.AdministrativeareaApi.service.administrativearea;

import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.AlreadyExistsException;
import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.MissingDataException;
import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.NotFoundException;
import com.wanfadger.AdministrativeareaApi.config.CacheValueKeyConfig;
import com.wanfadger.AdministrativeareaApi.dto.*;
import com.wanfadger.AdministrativeareaApi.dto.reponses.AdministrativeAreaResponseDto;
import com.wanfadger.AdministrativeareaApi.dto.uniqueDtos.*;
import com.wanfadger.AdministrativeareaApi.entity.*;
import com.wanfadger.AdministrativeareaApi.repository.*;
import com.wanfadger.AdministrativeareaApi.shared.util.CacheHelperService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdministrativeAreaServiceImpl implements AdministrativeAreaService {
    private final RegionRepository regionRepository;
    private final SubRegionRepository subRegionRepository;
    private final LocalGovernmentRepository localGovernmentRepository;
    private final CountyRepository countyRepository;
    private final SubCountyRepository subCountyRepository;
    private final ParishRepository parishRepository;
    private final CacheHelperService cacheHelper;

    private boolean notNullEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    private boolean nullEmpty(String value) {
        return value == null || value.isEmpty();
    }

    @Transactional(readOnly = true)
    private String generateCode(AdministrativeAreaType administrativeAreaType) {
        return switch (administrativeAreaType) {
            case REGION -> {
                String code;
                do {
                    code = UUID.randomUUID().toString();
                } while (regionRepository.findByCodeIgnoreCase(code).isPresent());
                yield code;
            }

            case SUBREGION -> {
                String code;
                do {
                    code = UUID.randomUUID().toString();
                } while (subRegionRepository.findByCodeIgnoreCase(code).isPresent());
                yield code;
            }

            case LOCALGOVERNMENT -> {
                String code;
                do {
                    code = UUID.randomUUID().toString();
                } while (localGovernmentRepository.findByCodeIgnoreCase(code).isPresent());
                yield code;
            }

            case COUNTY -> {
                String code;
                do {
                    code = UUID.randomUUID().toString();
                } while (countyRepository.findByCodeIgnoreCase(code).isPresent());
                yield code;
            }

            case SUBCOUNTY -> {
                String code;
                do {
                    code = UUID.randomUUID().toString();
                } while (subCountyRepository.findByCodeIgnoreCase(code).isPresent());
                yield code;
            }

            case PARISH -> {
                String code;
                do {
                    code = UUID.randomUUID().toString();
                } while (parishRepository.findByCodeIgnoreCase(code).isPresent());
                yield code;
            }
        };
    }

    @Override
    public ResponseEntity<AdministrativeAreaResponseDto<String>> newOne(Map<String, String> queryMap,
            NewAdministrativeAreaDTO dto) {
        AdministrativeAreaType administrativeAreaType = AdministrativeAreaType
                .fromStr(queryMap.get("type"))
                .orElseThrow(() -> new MissingDataException("Missing Administrative Area Type"));

        return switch (administrativeAreaType) {
            case REGION -> {

                if (regionRepository.findByNameIgnoreCase(dto.getName()).isPresent()) {
                    throw new AlreadyExistsException("Administrative Area Already Exists");
                }

                Region region = convertDtoRegion(dto, administrativeAreaType);

                regionRepository.save(region);

                // Evict service-level cache after write operation
                cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS);
                cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER);

                yield new ResponseEntity<>(
                        new AdministrativeAreaResponseDto<>(region.getCode(), "successfully created a region"),
                        HttpStatus.CREATED);
            }

            case SUBREGION -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode(region) for the sub region");
                }

                Region region = regionRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                        .orElseThrow(() -> new InvalidException("Invalid PartOfCode: " + dto.getPartOfCode()));

                if (subRegionRepository.findByNameIgnoreCaseAndRegion_Code(dto.getName(), dto.getPartOfCode())
                        .isPresent()) {
                    throw new AlreadyExistsException("Sub region Already Exists in the region");
                }

                SubRegion subRegion = convertDtoSubRegion(dto, administrativeAreaType);
                subRegion.setRegion(region);

                subRegionRepository.save(subRegion);

                // Evict service-level cache after write operation
                cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS);
                cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER);

                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>(subRegion.getCode(), "success"),
                        HttpStatus.CREATED);
            }

            case LOCALGOVERNMENT -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode(sub region) for localgovernment");
                }

                SubRegion subRegion = subRegionRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                        .orElseThrow(() -> new InvalidException("Invalid PartOfCode: " + dto.getPartOfCode()));

                if (localGovernmentRepository.findByNameIgnoreCaseAndSubRegion_Code(dto.getName(), dto.getPartOfCode())
                        .isPresent()) {
                    throw new AlreadyExistsException("Local Government Already Exists in the sub region");
                }

                LocalGovernment localGovernment = convertDtoLocalGovernment(dto, administrativeAreaType);
                localGovernment.setSubRegion(subRegion);

                localGovernmentRepository.save(localGovernment);

                // Evict service-level cache after write operation
                cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS);
                cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER);

                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>(localGovernment.getCode(), "success"),
                        HttpStatus.CREATED);
            }

            case COUNTY -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode(local government) for county");
                }

                LocalGovernment localGovernment = localGovernmentRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                        .orElseThrow(() -> new InvalidException("Invalid PartOfCode: " + dto.getPartOfCode()));

                if (countyRepository.findByNameIgnoreCaseAndLocalGovernment_Code(dto.getName(), dto.getPartOfCode())
                        .isPresent()) {
                    throw new AlreadyExistsException("County Already Exists in the local government");
                }

                County county = convertDtoCounty(dto, administrativeAreaType);
                county.setLocalGovernment(localGovernment);

                countyRepository.save(county);

                // Evict service-level cache after write operation
                cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS);
                cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER);

                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>(county.getCode(), "success"),
                        HttpStatus.CREATED);
            }

            case SUBCOUNTY -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode(county) for sub county");
                }

                County county = countyRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                        .orElseThrow(() -> new InvalidException("Invalid PartOfCode"));

                if (subCountyRepository.findByNameIgnoreCaseAndCounty_Id(dto.getName(), dto.getPartOfCode())
                        .isPresent()) {
                    throw new AlreadyExistsException("Sub County Already Exists in the county");
                }

                SubCounty subCounty = convertDtoSubCounty(dto, administrativeAreaType);
                subCounty.setCounty(county);

                subCountyRepository.save(subCounty);

                // Evict service-level cache after write operation
                cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS);
                cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER);

                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>(subCounty.getCode(), "success"),
                        HttpStatus.CREATED);
            }

            case PARISH -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode(sub county) for parish");
                }

                SubCounty subCounty = subCountyRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                        .orElseThrow(() -> new InvalidException("Invalid PartOfCode: " + dto.getPartOfCode()));

                if (parishRepository.findByNameIgnoreCaseAndSubCounty_Code(dto.getName(), dto.getPartOfCode())
                        .isPresent()) {
                    throw new AlreadyExistsException("Parish Already Exists in the sub county");
                }

                Parish parish = convertDtoParish(dto, administrativeAreaType);
                parish.setSubCounty(subCounty);

                parishRepository.save(parish);

                // Evict service-level cache after write operation
                cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS);
                cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER);

                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>(parish.getCode(), "success"),
                        HttpStatus.CREATED);
            }
        };
    }

    private Parish convertDtoParish(NewAdministrativeAreaDTO dto, AdministrativeAreaType administrativeAreaType) {
        Parish parish = new Parish();
        parish.setName(dto.getName());
        parish.setLatitude(dto.getLatitude() != null ? Double.valueOf(dto.getLatitude()) : null);
        parish.setLongitude(dto.getLongitude() != null ? Double.valueOf(dto.getLongitude()) : null);
        parish.setCode(generateCode(administrativeAreaType));
        return parish;
    }

    private SubCounty convertDtoSubCounty(NewAdministrativeAreaDTO dto, AdministrativeAreaType administrativeAreaType) {
        SubCounty subCounty = new SubCounty();
        subCounty.setName(dto.getName());
        subCounty.setLatitude(dto.getLatitude() != null ? Double.valueOf(dto.getLatitude()) : null);
        subCounty.setLongitude(dto.getLongitude() != null ? Double.valueOf(dto.getLongitude()) : null);
        subCounty.setCode(generateCode(administrativeAreaType));
        return subCounty;
    }

    private County convertDtoCounty(NewAdministrativeAreaDTO dto, AdministrativeAreaType administrativeAreaType) {
        County county = new County();
        county.setName(dto.getName());
        county.setLatitude(dto.getLatitude() != null ? Double.valueOf(dto.getLatitude()) : null);
        county.setLongitude(dto.getLongitude() != null ? Double.valueOf(dto.getLongitude()) : null);
        county.setCode(generateCode(administrativeAreaType));
        return county;
    }

    private LocalGovernment convertDtoLocalGovernment(NewAdministrativeAreaDTO dto,
            AdministrativeAreaType administrativeAreaType) {
        LocalGovernment localGovernment = new LocalGovernment();
        localGovernment.setName(dto.getName());
        localGovernment.setLatitude(dto.getLatitude() != null ? Double.valueOf(dto.getLatitude()) : null);
        localGovernment.setLongitude(dto.getLongitude() != null ? Double.valueOf(dto.getLongitude()) : null);
        localGovernment.setCode(generateCode(administrativeAreaType));
        return localGovernment;
    }

    private SubRegion convertDtoSubRegion(NewAdministrativeAreaDTO dto, AdministrativeAreaType administrativeAreaType) {
        SubRegion subRegion = new SubRegion();
        subRegion.setName(dto.getName());
        subRegion.setLatitude(notNullEmpty(dto.getLatitude()) ? Double.valueOf(dto.getLatitude()) : null);
        subRegion.setLongitude(notNullEmpty(dto.getLongitude()) ? Double.valueOf(dto.getLongitude()) : null);
        subRegion.setCode(generateCode(administrativeAreaType));
        return subRegion;
    }

    private Region convertDtoRegion(NewAdministrativeAreaDTO dto, AdministrativeAreaType administrativeAreaType) {
        Region region = new Region();
        region.setName(dto.getName());
        region.setDescription(dto.getDescription());
        region.setLatitude(dto.getLatitude() != null ? Double.valueOf(dto.getLatitude()) : null);
        region.setLongitude(dto.getLongitude() != null ? Double.valueOf(dto.getLongitude()) : null);
        region.setCode(generateCode(administrativeAreaType));
        return region;
    }

    @Override
    public ResponseEntity<AdministrativeAreaResponseDto<String>> newList(Map<String, String> queryMap,
            List<NewAdministrativeAreaDTO> dtos) {
        Optional<AdministrativeAreaType> optionalAdministrativeAreaType = AdministrativeAreaType
                .fromStr(queryMap.get("type"));
        if (optionalAdministrativeAreaType.isEmpty()) {
            throw new MissingDataException("Missing Administrative Area Type");
        }

        AdministrativeAreaType administrativeAreaType = optionalAdministrativeAreaType.get();

        return switch (administrativeAreaType) {
            case REGION -> {
                // exclude existing ones
                List<Region> regions = dtos.parallelStream()
                        .filter(dto -> regionRepository.findByNameIgnoreCase(dto.getName()).isEmpty())
                        .map(dto -> convertDtoRegion(dto, administrativeAreaType)).toList();

                regionRepository.saveAll(regions);

                // Evict service-level cache after write operation
                cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS);
                cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER);

                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success",
                        "successfully added " + regions.size() + " administrative areas"), HttpStatus.CREATED);

            }
            case SUBREGION -> {

                // check if all have PartOfCode
                if (dtos.parallelStream().anyMatch(dto -> !notNullEmpty(dto.getPartOfCode()))) {
                    throw new MissingDataException("Found Administrative Area without PartOfCoce");
                }

                // exclude existing ones
                List<SubRegion> subRegions = dtos.parallelStream().filter(
                        dto -> subRegionRepository
                                .findByNameIgnoreCaseAndRegion_Code(dto.getName(), dto.getPartOfCode()).isEmpty())
                        .map(dto -> convertDtoSubRegion(dto, administrativeAreaType)).toList();

                subRegionRepository.saveAll(subRegions);

                // Evict service-level cache after write operation
                cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS);
                cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER);

                yield new ResponseEntity<>(
                        new AdministrativeAreaResponseDto<>("success",
                                "successfully added " + subRegions.size() + " administrative areas"),
                        HttpStatus.CREATED);
            }
            case LOCALGOVERNMENT -> {

                // check if all have PartOfCode
                if (dtos.parallelStream().anyMatch(dto -> !notNullEmpty(dto.getPartOfCode()))) {
                    throw new MissingDataException("Found Administrative Area without PartOfCoce");
                }

                // exclude existing ones
                List<LocalGovernment> localGovernments = dtos.parallelStream()
                        .filter(dto -> localGovernmentRepository
                                .findByNameIgnoreCaseAndSubRegion_Code(dto.getName(), dto.getPartOfCode()).isEmpty())
                        .map(dto -> convertDtoLocalGovernment(dto, administrativeAreaType)).toList();

                localGovernmentRepository.saveAll(localGovernments);

                // Evict service-level cache after write operation
                cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS);
                cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER);

                yield new ResponseEntity<>(
                        new AdministrativeAreaResponseDto<>("success",
                                "successfully added " + localGovernments.size() + " administrative areas"),
                        HttpStatus.CREATED);
            }

            case COUNTY -> {

                // check if all have PartOfCode
                if (dtos.parallelStream().anyMatch(dto -> !notNullEmpty(dto.getPartOfCode()))) {
                    throw new MissingDataException("Found Administrative Area without PartOfCoce");
                }

                // exclude existing ones
                List<County> counties = dtos
                        .parallelStream().filter(dto -> countyRepository
                                .findByNameIgnoreCaseAndLocalGovernment_Code(dto.getName(), dto.getPartOfCode())
                                .isEmpty())
                        .map(dto -> convertDtoCounty(dto, administrativeAreaType)).toList();

                countyRepository.saveAll(counties);

                // Evict service-level cache after write operation
                cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS);
                cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER);

                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success",
                        "successfully added " + counties.size() + " administrative areas"), HttpStatus.CREATED);
            }

            case SUBCOUNTY -> {

                // check if all have PartOfCode
                if (dtos.parallelStream().anyMatch(dto -> !notNullEmpty(dto.getPartOfCode()))) {
                    throw new MissingDataException("Found Administrative Area without PartOfCoce");
                }

                // exclude existing ones
                List<SubCounty> subCounties = dtos.parallelStream().filter(
                        dto -> subCountyRepository.findByNameIgnoreCaseAndCounty_Id(dto.getName(), dto.getPartOfCode())
                                .isEmpty())
                        .map(dto -> convertDtoSubCounty(dto, administrativeAreaType)).toList();

                subCountyRepository.saveAll(subCounties);

                // Evict service-level cache after write operation
                cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS);
                cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER);

                yield new ResponseEntity<>(
                        new AdministrativeAreaResponseDto<>("success",
                                "successfully added " + subCounties.size() + " administrative areas"),
                        HttpStatus.CREATED);
            }

            case PARISH -> {

                // check if all have PartOfCode
                if (dtos.parallelStream().anyMatch(dto -> !notNullEmpty(dto.getPartOfCode()))) {
                    throw new MissingDataException("Found Administrative Area without PartOfCoce");
                }

                // exclude existing ones
                List<Parish> parishes = dtos.parallelStream().filter(
                        dto -> parishRepository
                                .findByNameIgnoreCaseAndSubCounty_Code(dto.getName(), dto.getPartOfCode()).isEmpty())
                        .map(dto -> convertDtoParish(dto, administrativeAreaType)).toList();

                parishRepository.saveAll(parishes);

                // Evict service-level cache after write operation
                cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS);
                cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER);

                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success",
                        "successfully added " + parishes.size() + " administrative areas"), HttpStatus.CREATED);

            }
        };

    }

    @Override
    public AdministrativeAreaResponseDto<CodeNameDTO> filterOne(Map<String, String> queryMap) {
        String cacheKey = CacheHelperService.generateKey(queryMap);
        String type = queryMap.get("type");
        String code = queryMap.get("code");

        if (!notNullEmpty(type)) {
            throw new MissingDataException("Missing Administrative Area Type");
        }

        Optional<AdministrativeAreaType> optionalAdministrativeAreaType = AdministrativeAreaType.fromStr(type);
        if (optionalAdministrativeAreaType.isEmpty()) {
            throw new MissingDataException("Missing Administrative Area Type");
        }

        AdministrativeAreaType administrativeAreaType = optionalAdministrativeAreaType.get();

        // Check cache and fetch from database using independent switch blocks
        switch (administrativeAreaType) {
            case REGION -> {
                // Check cache first
                ParameterizedTypeReference<AdministrativeAreaResponseDto<CodeNameDTO>> typeRef = new ParameterizedTypeReference<AdministrativeAreaResponseDto<CodeNameDTO>>() {
                };
                AdministrativeAreaResponseDto<CodeNameDTO> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                if (!notNullEmpty(code)) {
                    throw new MissingDataException("Missing Administrative Area Code");
                }
                Region region = regionRepository.findByCodeIgnoreCase(code)
                        .orElseThrow(() -> new NotFoundException("Region not found"));
                AdministrativeAreaResponseDto<CodeNameDTO> result = new AdministrativeAreaResponseDto<>(
                        new CodeNameDTO(region.getCode(), region.getName()));

                // Cache the result (7 days TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, cacheKey, result, 7,
                            TimeUnit.DAYS);
                }

                return result;
            }
            case SUBREGION -> {
                // Check cache first
                ParameterizedTypeReference<AdministrativeAreaResponseDto<CodeNameDTO>> typeRef = new ParameterizedTypeReference<AdministrativeAreaResponseDto<CodeNameDTO>>() {
                };
                AdministrativeAreaResponseDto<CodeNameDTO> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                if (!notNullEmpty(code)) {
                    throw new MissingDataException("Missing Administrative Area Code");
                }
                SubRegion subRegion = subRegionRepository.findByCodeIgnoreCase(code)
                        .orElseThrow(() -> new NotFoundException("SubRegion not found"));
                AdministrativeAreaResponseDto<CodeNameDTO> result = new AdministrativeAreaResponseDto<>(
                        new CodeNameDTO(subRegion.getCode(), subRegion.getName()));

                // Cache the result (7 days TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, cacheKey, result, 7,
                            TimeUnit.DAYS);
                }

                return result;
            }
            case LOCALGOVERNMENT -> {
                // Check cache first
                ParameterizedTypeReference<AdministrativeAreaResponseDto<CodeNameDTO>> typeRef = new ParameterizedTypeReference<AdministrativeAreaResponseDto<CodeNameDTO>>() {
                };
                AdministrativeAreaResponseDto<CodeNameDTO> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                if (!notNullEmpty(code)) {
                    throw new MissingDataException("Missing Administrative Area Code");
                }
                LocalGovernment localGovernment = localGovernmentRepository.findByCodeIgnoreCase(code)
                        .orElseThrow(() -> new NotFoundException("LocalGovernment not found"));
                AdministrativeAreaResponseDto<CodeNameDTO> result = new AdministrativeAreaResponseDto<>(
                        new CodeNameDTO(localGovernment.getCode(), localGovernment.getName()));

                // Cache the result (7 days TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, cacheKey, result, 7,
                            TimeUnit.DAYS);
                }

                return result;
            }
            case COUNTY -> {
                // Check cache first
                ParameterizedTypeReference<AdministrativeAreaResponseDto<CodeNameDTO>> typeRef = new ParameterizedTypeReference<AdministrativeAreaResponseDto<CodeNameDTO>>() {
                };
                AdministrativeAreaResponseDto<CodeNameDTO> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                if (!notNullEmpty(code)) {
                    throw new MissingDataException("Missing Administrative Area Code");
                }
                County county = countyRepository.findByCodeIgnoreCase(code)
                        .orElseThrow(() -> new NotFoundException("County not found"));
                AdministrativeAreaResponseDto<CodeNameDTO> result = new AdministrativeAreaResponseDto<>(
                        new CodeNameDTO(county.getCode(), county.getName()));

                // Cache the result (7 days TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, cacheKey, result, 7,
                            TimeUnit.DAYS);
                }

                return result;
            }
            case SUBCOUNTY -> {
                // Check cache first
                ParameterizedTypeReference<AdministrativeAreaResponseDto<CodeNameDTO>> typeRef = new ParameterizedTypeReference<AdministrativeAreaResponseDto<CodeNameDTO>>() {
                };
                AdministrativeAreaResponseDto<CodeNameDTO> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                if (!notNullEmpty(code)) {
                    throw new MissingDataException("Missing Administrative Area Code");
                }
                SubCounty subCounty = subCountyRepository.findByCodeIgnoreCase(code)
                        .orElseThrow(() -> new NotFoundException("SubCounty not found"));
                AdministrativeAreaResponseDto<CodeNameDTO> result = new AdministrativeAreaResponseDto<>(
                        new CodeNameDTO(subCounty.getCode(), subCounty.getName()));

                // Cache the result (7 days TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, cacheKey, result, 7,
                            TimeUnit.DAYS);
                }

                return result;
            }
            case PARISH -> {
                // Check cache first
                ParameterizedTypeReference<AdministrativeAreaResponseDto<CodeNameDTO>> typeRef = new ParameterizedTypeReference<AdministrativeAreaResponseDto<CodeNameDTO>>() {
                };
                AdministrativeAreaResponseDto<CodeNameDTO> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                if (!notNullEmpty(code)) {
                    throw new MissingDataException("Missing Administrative Area Code");
                }
                Parish parish = parishRepository.findByCodeIgnoreCase(code)
                        .orElseThrow(() -> new NotFoundException("Parish not found"));
                AdministrativeAreaResponseDto<CodeNameDTO> result = new AdministrativeAreaResponseDto<>(
                        new CodeNameDTO(parish.getCode(), parish.getName()));

                // Cache the result (7 days TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, cacheKey, result, 7,
                            TimeUnit.DAYS);
                }

                return result;
            }
            default ->
                throw new MissingDataException("Unsupported Administrative Area Type: " + administrativeAreaType);
        }
    }

    @Override
    public AdministrativeAreaResponseDto<List<CodeNameDTO>> filterList(Map<String, String> queryMap) {
        String cacheKey = CacheHelperService.generateKey(queryMap);
        String type = queryMap.get("type");
        String partOf = queryMap.get("partOf");

        if (!notNullEmpty(type)) {
            throw new MissingDataException("Missing Administrative Area Type");
        }

        Optional<AdministrativeAreaType> optionalAdministrativeAreaType = AdministrativeAreaType.fromStr(type);
        if (optionalAdministrativeAreaType.isEmpty()) {
            throw new MissingDataException("Missing Administrative Area Type");
        }

        AdministrativeAreaType administrativeAreaType = optionalAdministrativeAreaType.get();

        // Check cache and fetch from database using independent switch blocks
        // All cases return the same type (CodeNameDTO), so we use the same
        // ParameterizedTypeReference
        ParameterizedTypeReference<AdministrativeAreaResponseDto<List<CodeNameDTO>>> typeRef = new ParameterizedTypeReference<AdministrativeAreaResponseDto<List<CodeNameDTO>>>() {
        };

        switch (administrativeAreaType) {
            case REGION -> {
                AdministrativeAreaResponseDto<List<CodeNameDTO>> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                // Cache miss - fetch from database
                List<CodeNameDTO> codeNameDtoList = regionRepository.findAll().parallelStream()
                        .map(region -> new CodeNameDTO(region.getCode(), region.getName()))
                        .sorted(Comparator.comparing(CodeNameDTO::getCode))
                        .toList();
                AdministrativeAreaResponseDto<List<CodeNameDTO>> result = new AdministrativeAreaResponseDto<>(
                        codeNameDtoList);

                // Cache the result (7 days TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, cacheKey, result, 7,
                            TimeUnit.DAYS);
                }

                return result;
            }
            case SUBREGION -> {
                AdministrativeAreaResponseDto<List<CodeNameDTO>> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                if (!notNullEmpty(partOf)) {
                    throw new MissingDataException("Missing Administrative Area partOf");
                }

                List<CodeNameDTO> codeNameDtoList = subRegionRepository.findAllByRegion_Code(partOf).parallelStream()
                        .map(subRegion -> new CodeNameDTO(subRegion.getCode(), subRegion.getName()))
                        .sorted(Comparator.comparing(CodeNameDTO::getCode))
                        .toList();
                AdministrativeAreaResponseDto<List<CodeNameDTO>> result = new AdministrativeAreaResponseDto<>(
                        codeNameDtoList);

                // Cache the result (7 days TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, cacheKey, result, 7,
                            TimeUnit.DAYS);
                }

                return result;
            }
            case LOCALGOVERNMENT -> {
                AdministrativeAreaResponseDto<List<CodeNameDTO>> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                if (!notNullEmpty(partOf)) {
                    throw new MissingDataException("Missing Administrative Area partOf");
                }

                List<CodeNameDTO> codeNameDtoList = localGovernmentRepository.findAllBySubRegion_Code(partOf)
                        .parallelStream()
                        .map(localGovernment -> new CodeNameDTO(localGovernment.getCode(), localGovernment.getName()))
                        .sorted(Comparator.comparing(CodeNameDTO::getCode))
                        .toList();
                AdministrativeAreaResponseDto<List<CodeNameDTO>> result = new AdministrativeAreaResponseDto<>(
                        codeNameDtoList);

                // Cache the result (7 days TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, cacheKey, result, 7,
                            TimeUnit.DAYS);
                }

                return result;
            }
            case COUNTY -> {
                AdministrativeAreaResponseDto<List<CodeNameDTO>> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                if (!notNullEmpty(partOf)) {
                    throw new MissingDataException("Missing Administrative Area partOf");
                }

                List<CodeNameDTO> codeNameDtoList = countyRepository.findAllByLocalGovernment_Code(partOf)
                        .parallelStream()
                        .map(county -> new CodeNameDTO(county.getCode(), county.getName()))
                        .sorted(Comparator.comparing(CodeNameDTO::getCode))
                        .toList();
                AdministrativeAreaResponseDto<List<CodeNameDTO>> result = new AdministrativeAreaResponseDto<>(
                        codeNameDtoList);

                // Cache the result (7 days TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, cacheKey, result, 7,
                            TimeUnit.DAYS);
                }

                return result;
            }
            case SUBCOUNTY -> {
                AdministrativeAreaResponseDto<List<CodeNameDTO>> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                if (!notNullEmpty(partOf)) {
                    throw new MissingDataException("Missing Administrative Area partOf");
                }

                List<CodeNameDTO> codeNameDtoList = subCountyRepository.findAllByCounty_Code(partOf).parallelStream()
                        .map(subCounty -> new CodeNameDTO(subCounty.getCode(), subCounty.getName()))
                        .sorted(Comparator.comparing(CodeNameDTO::getCode))
                        .toList();
                AdministrativeAreaResponseDto<List<CodeNameDTO>> result = new AdministrativeAreaResponseDto<>(
                        codeNameDtoList);

                // Cache the result (7 days TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, cacheKey, result, 7,
                            TimeUnit.DAYS);
                }

                return result;
            }
            case PARISH -> {
                AdministrativeAreaResponseDto<List<CodeNameDTO>> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                if (!notNullEmpty(partOf)) {
                    throw new MissingDataException("Missing Administrative Area partOf");
                }

                List<CodeNameDTO> codeNameDtoList = parishRepository.findAllBySubCounty_Code(partOf).parallelStream()
                        .map(parish -> new CodeNameDTO(parish.getCode(), parish.getName()))
                        .sorted(Comparator.comparing(CodeNameDTO::getCode))
                        .toList();
                AdministrativeAreaResponseDto<List<CodeNameDTO>> result = new AdministrativeAreaResponseDto<>(
                        codeNameDtoList);

                // Cache the result (7 days TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, cacheKey, result, 7,
                            TimeUnit.DAYS);
                }

                return result;
            }
            default ->
                throw new MissingDataException("Unsupported Administrative Area Type: " + administrativeAreaType);
        }
    }

    @Override
    public AdministrativeAreaResponseDto<List<CodeNameDTO>> getParishByPartOf(Map<String, String> queryMap) {
        String cacheKey = CacheHelperService.generateKey(queryMap);
        String type = queryMap.get("type");
        String partOfCode = queryMap.get("partOfCode");

        if (!notNullEmpty(type)) {
            throw new MissingDataException("Missing Administrative Area Type");
        }

        Optional<AdministrativeAreaType> optionalAdministrativeAreaType = AdministrativeAreaType.fromStr(type);
        if (optionalAdministrativeAreaType.isEmpty()) {
            throw new MissingDataException("Missing Administrative Area Type");
        }

        AdministrativeAreaType administrativeAreaType = optionalAdministrativeAreaType.get();

        // Check cache and fetch from database using independent switch blocks
        // All cases return the same type (CodeNameDTO), so we use the same
        // ParameterizedTypeReference
        ParameterizedTypeReference<AdministrativeAreaResponseDto<List<CodeNameDTO>>> typeRef = new ParameterizedTypeReference<AdministrativeAreaResponseDto<List<CodeNameDTO>>>() {
        };

        switch (administrativeAreaType) {
            case REGION -> {
                AdministrativeAreaResponseDto<List<CodeNameDTO>> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                // Get all parishes under region hierarchy
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

                AdministrativeAreaResponseDto<List<CodeNameDTO>> result = new AdministrativeAreaResponseDto<>(
                        codeNameDtoList);

                // Cache the result (7 days TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, result, 7, TimeUnit.DAYS);
                }

                return result;
            }
            case SUBREGION -> {
                AdministrativeAreaResponseDto<List<CodeNameDTO>> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                if (!notNullEmpty(partOfCode)) {
                    throw new MissingDataException("Missing Administrative Area partOf");
                }

                // Get all parishes under sub-region hierarchy
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

                AdministrativeAreaResponseDto<List<CodeNameDTO>> result = new AdministrativeAreaResponseDto<>(
                        codeNameDtoList);

                // Cache the result (7 days TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, result, 7, TimeUnit.DAYS);
                }

                return result;
            }
            case LOCALGOVERNMENT -> {
                AdministrativeAreaResponseDto<List<CodeNameDTO>> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                if (!notNullEmpty(partOfCode)) {
                    throw new MissingDataException("Missing Administrative Area partOf");
                }

                // Get all parishes under local government hierarchy
                List<String> countyCodes = countyRepository.findAllByLocalGovernment_Code(partOfCode).parallelStream()
                        .map(County::getCode).distinct().toList();
                List<String> subCountyCodes = subCountyRepository.findAllByCountyCodes(countyCodes).parallelStream()
                        .map(SubCounty::getCode).distinct().toList();
                List<CodeNameDTO> codeNameDtoList = parishRepository.findAllBySubCountyCodes(subCountyCodes)
                        .parallelStream()
                        .map(parish -> new CodeNameDTO(parish.getCode(), parish.getName()))
                        .sorted(Comparator.comparing(CodeNameDTO::getCode))
                        .toList();

                AdministrativeAreaResponseDto<List<CodeNameDTO>> result = new AdministrativeAreaResponseDto<>(
                        codeNameDtoList);

                // Cache the result (7 days TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, result, 7, TimeUnit.DAYS);
                }

                return result;
            }
            case COUNTY -> {
                AdministrativeAreaResponseDto<List<CodeNameDTO>> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                if (!notNullEmpty(partOfCode)) {
                    throw new MissingDataException("Missing Administrative Area partOf");
                }

                // Get all parishes under county
                List<String> subCountyCodes = subCountyRepository.findAllByCounty_Code(partOfCode).parallelStream()
                        .map(SubCounty::getCode).distinct().toList();
                List<CodeNameDTO> codeNameDtoList = parishRepository.findAllBySubCountyCodes(subCountyCodes)
                        .parallelStream()
                        .map(parish -> new CodeNameDTO(parish.getCode(), parish.getName()))
                        .sorted(Comparator.comparing(CodeNameDTO::getCode))
                        .toList();

                AdministrativeAreaResponseDto<List<CodeNameDTO>> result = new AdministrativeAreaResponseDto<>(
                        codeNameDtoList);

                // Cache the result (7 days TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, result, 7, TimeUnit.DAYS);
                }

                return result;
            }
            case SUBCOUNTY -> {
                AdministrativeAreaResponseDto<List<CodeNameDTO>> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                if (!notNullEmpty(partOfCode)) {
                    throw new MissingDataException("Missing Administrative Area partOf");
                }

                List<CodeNameDTO> codeNameDtoList = parishRepository.findAllBySubCounty_Code(partOfCode)
                        .parallelStream()
                        .map(parish -> new CodeNameDTO(parish.getCode(), parish.getName()))
                        .sorted(Comparator.comparing(CodeNameDTO::getCode))
                        .toList();

                AdministrativeAreaResponseDto<List<CodeNameDTO>> result = new AdministrativeAreaResponseDto<>(
                        codeNameDtoList);

                // Cache the result (7 days TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, result, 7, TimeUnit.DAYS);
                }

                return result;
            }
            case PARISH -> {
                // No parishes under a parish
                AdministrativeAreaResponseDto<List<CodeNameDTO>> result = new AdministrativeAreaResponseDto<>(
                        Collections.emptyList());

                // Cache the result (7 days TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, result, 7, TimeUnit.DAYS);
                }

                return result;
            }
            default ->
                throw new MissingDataException("Unsupported Administrative Area Type: " + administrativeAreaType);
        }
    }

    @Override
    public AdministrativeAreaResponseDto<?> searchList(Map<String, String> queryMap) {
        log.info("Searching for administrative areas with query map: {}", queryMap);
        String cacheKey = CacheHelperService.generateKey(queryMap);
        log.info("Cache key: {}", cacheKey);
        String type = queryMap.get("type");
        String partOf = queryMap.get("partOf");

        AdministrativeAreaType administrativeAreaType = AdministrativeAreaType.fromStr(type)
                .orElseThrow(() -> new MissingDataException("Unsupported Administrative Area Type: " + type));

        // Use type-specific ParameterizedTypeReference in each case to preserve all
        // data
        switch (administrativeAreaType) {
            case REGION -> {
                // Check cache with specific type
                ParameterizedTypeReference<AdministrativeAreaResponseDto<List<RegionDTO>>> typeRef = new ParameterizedTypeReference<AdministrativeAreaResponseDto<List<RegionDTO>>>() {
                };
                AdministrativeAreaResponseDto<List<RegionDTO>> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                List<RegionDTO> regionDtos = regionRepository.findAll().parallelStream()
                        .map(AdministrativeAreaServiceImpl::convertRegionDTO)
                        .sorted(Comparator.comparing(RegionDTO::getCode))
                        .toList();
                AdministrativeAreaResponseDto<List<RegionDTO>> result = new AdministrativeAreaResponseDto<>(regionDtos);

                // Cache the result (1 hour TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, result, 1, TimeUnit.HOURS);
                }

                return result;
            }
            case SUBREGION -> {
                // Check cache with specific type
                ParameterizedTypeReference<AdministrativeAreaResponseDto<List<SubRegionDTO>>> typeRef = new ParameterizedTypeReference<AdministrativeAreaResponseDto<List<SubRegionDTO>>>() {
                };
                AdministrativeAreaResponseDto<List<SubRegionDTO>> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                List<SubRegionDTO> subRegionDTOs;
                if (notNullEmpty(partOf)) {
                    subRegionDTOs = subRegionRepository.findAllByRegion_Code(partOf).parallelStream()
                            .map(AdministrativeAreaServiceImpl::convertSubRegionDTO)
                            .sorted(Comparator.comparing(SubRegionDTO::getCode))
                            .toList();
                } else {
                    subRegionDTOs = subRegionRepository.findAll().parallelStream()
                            .map(AdministrativeAreaServiceImpl::convertSubRegionDTO)
                            .sorted(Comparator.comparing(SubRegionDTO::getCode))
                            .toList();
                }
                AdministrativeAreaResponseDto<List<SubRegionDTO>> result = new AdministrativeAreaResponseDto<>(
                        subRegionDTOs);

                // Cache the result (1 hour TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, result, 1, TimeUnit.HOURS);
                }

                return result;
            }
            case LOCALGOVERNMENT -> {
                // Check cache with specific type
                ParameterizedTypeReference<AdministrativeAreaResponseDto<List<LocalGovernmentDTO>>> typeRef = new ParameterizedTypeReference<AdministrativeAreaResponseDto<List<LocalGovernmentDTO>>>() {
                };
                AdministrativeAreaResponseDto<List<LocalGovernmentDTO>> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                List<LocalGovernmentDTO> localGovernmentDtos;
                if (notNullEmpty(partOf)) {
                    localGovernmentDtos = localGovernmentRepository.findAllBySubRegion_Code(partOf).parallelStream()
                            .map(this::convertLocalGovernmentDTO)
                            .sorted(Comparator.comparing(LocalGovernmentDTO::getCode))
                            .toList();
                } else {
                    localGovernmentDtos = localGovernmentRepository.findAll().parallelStream()
                            .map(this::convertLocalGovernmentDTO)
                            .sorted(Comparator.comparing(LocalGovernmentDTO::getCode))
                            .toList();
                }
                AdministrativeAreaResponseDto<List<LocalGovernmentDTO>> result = new AdministrativeAreaResponseDto<>(
                        localGovernmentDtos);

                // Cache the result (1 hour TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, result, 1, TimeUnit.HOURS);
                }

                return result;
            }
            case COUNTY -> {
                // Check cache with specific type
                ParameterizedTypeReference<AdministrativeAreaResponseDto<List<CountyDTO>>> typeRef = new ParameterizedTypeReference<AdministrativeAreaResponseDto<List<CountyDTO>>>() {
                };
                AdministrativeAreaResponseDto<List<CountyDTO>> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                List<CountyDTO> countyDtos;
                if (notNullEmpty(partOf)) {
                    countyDtos = countyRepository.findAllByLocalGovernment_Code(partOf).parallelStream()
                            .map(this::convertCountyDTO)
                            .sorted(Comparator.comparing(CountyDTO::getCode))
                            .toList();
                } else {
                    countyDtos = countyRepository.findAll().parallelStream()
                            .map(this::convertCountyDTO)
                            .sorted(Comparator.comparing(CountyDTO::getCode))
                            .toList();
                }
                AdministrativeAreaResponseDto<List<CountyDTO>> result = new AdministrativeAreaResponseDto<>(countyDtos);

                // Cache the result (1 hour TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, result, 1, TimeUnit.HOURS);
                }

                return result;
            }
            case SUBCOUNTY -> {
                // Check cache with specific type
                ParameterizedTypeReference<AdministrativeAreaResponseDto<List<SubCountyDTO>>> typeRef = new ParameterizedTypeReference<AdministrativeAreaResponseDto<List<SubCountyDTO>>>() {
                };
                AdministrativeAreaResponseDto<List<SubCountyDTO>> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                List<SubCountyDTO> subCountyDTOs;
                if (notNullEmpty(partOf)) {
                    subCountyDTOs = subCountyRepository.findAllByCounty_Code(partOf).parallelStream()
                            .map(this::convertSubCountyDTO)
                            .sorted(Comparator.comparing(SubCountyDTO::getCode))
                            .toList();
                } else {
                    subCountyDTOs = subCountyRepository.findAll().parallelStream()
                            .map(this::convertSubCountyDTO)
                            .sorted(Comparator.comparing(SubCountyDTO::getCode))
                            .toList();
                }
                AdministrativeAreaResponseDto<List<SubCountyDTO>> result = new AdministrativeAreaResponseDto<>(
                        subCountyDTOs);

                // Cache the result (1 hour TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, result, 1, TimeUnit.HOURS);
                }

                return result;
            }
            case PARISH -> {
                long startTime = System.currentTimeMillis();

                // Check cache with specific type
                ParameterizedTypeReference<AdministrativeAreaResponseDto<List<ParishDTO>>> typeRef = new ParameterizedTypeReference<AdministrativeAreaResponseDto<List<ParishDTO>>>() {
                };
                AdministrativeAreaResponseDto<List<ParishDTO>> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, typeRef);

                if (cached != null) {
                    long cacheHitTime = System.currentTimeMillis() - startTime;
                    log.info("PARISH cache HIT - Response time: {}ms, Cache key: {}, Records: {}",
                            cacheHitTime, cacheKey, cached.getData() != null ? ((List<?>) cached.getData()).size() : 0);
                    return cached;
                }

                // Cache miss - fetch from database
                long dbStartTime = System.currentTimeMillis();
                List<ParishDTO> parishDtos;
                if (notNullEmpty(partOf)) {
                    parishDtos = parishRepository.findAllBySubCounty_Code(partOf).parallelStream()
                            .map(this::convertParishDTO)
                            .sorted(Comparator.comparing(ParishDTO::getCode))
                            .toList();
                } else {
                    parishDtos = parishRepository.findAll().parallelStream()
                            .map(this::convertParishDTO)
                            .sorted(Comparator.comparing(ParishDTO::getCode))
                            .toList();
                }
                long dbFetchTime = System.currentTimeMillis() - dbStartTime;

                AdministrativeAreaResponseDto<List<ParishDTO>> result = new AdministrativeAreaResponseDto<>(parishDtos);

                // Cache the result (1 hour TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, result, 1, TimeUnit.HOURS);
                }

                long totalTime = System.currentTimeMillis() - startTime;
                log.info(
                        "PARISH cache MISS - Total response time: {}ms (DB fetch: {}ms, Processing: {}ms), Cache key: {}, Records: {}",
                        totalTime, dbFetchTime, totalTime - dbFetchTime, cacheKey, parishDtos.size());

                return result;
            }
            default ->
                throw new MissingDataException("Unsupported Administrative Area Type: " + administrativeAreaType);
        }
    }

    @Override
    public AdministrativeAreaResponseDto<?> searchOne(Map<String, String> queryMap) {
        String cacheKey = CacheHelperService.generateKey(queryMap);
        String type = queryMap.get("type");
        String code = queryMap.get("code");

        if (code == null) {
            throw new MissingDataException("Missing required data");
        }

        Optional<AdministrativeAreaType> optionalAdministrativeAreaType = AdministrativeAreaType.fromStr(type);
        if (optionalAdministrativeAreaType.isEmpty()) {
            throw new MissingDataException("Missing Administrative Area Type");
        }

        AdministrativeAreaType administrativeAreaType = optionalAdministrativeAreaType.get();

        // Use type-specific ParameterizedTypeReference in each case to preserve all
        // data
        switch (administrativeAreaType) {
            case REGION -> {
                // Check cache with specific type
                ParameterizedTypeReference<AdministrativeAreaResponseDto<RegionDTO>> typeRef = new ParameterizedTypeReference<AdministrativeAreaResponseDto<RegionDTO>>() {
                };
                AdministrativeAreaResponseDto<RegionDTO> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                Region region = regionRepository.findByCodeIgnoreCase(code)
                        .orElseThrow(() -> new NotFoundException("Region not found"));
                AdministrativeAreaResponseDto<RegionDTO> result = new AdministrativeAreaResponseDto<>(
                        convertRegionDTO(region));

                // Cache the result (1 hour TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, result, 1, TimeUnit.HOURS);
                }

                return result;
            }
            case SUBREGION -> {
                // Check cache with specific type
                ParameterizedTypeReference<AdministrativeAreaResponseDto<SubRegionDTO>> typeRef = new ParameterizedTypeReference<AdministrativeAreaResponseDto<SubRegionDTO>>() {
                };
                AdministrativeAreaResponseDto<SubRegionDTO> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                SubRegion subRegion = subRegionRepository.findByCodeIgnoreCase(code)
                        .orElseThrow(() -> new NotFoundException("SubRegion not found"));
                AdministrativeAreaResponseDto<SubRegionDTO> result = new AdministrativeAreaResponseDto<>(
                        convertSubRegionDTO(subRegion));

                // Cache the result (1 hour TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, result, 1, TimeUnit.HOURS);
                }

                return result;
            }
            case LOCALGOVERNMENT -> {
                // Check cache with specific type
                ParameterizedTypeReference<AdministrativeAreaResponseDto<LocalGovernmentDTO>> typeRef = new ParameterizedTypeReference<AdministrativeAreaResponseDto<LocalGovernmentDTO>>() {
                };
                AdministrativeAreaResponseDto<LocalGovernmentDTO> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                LocalGovernment localGovernment = localGovernmentRepository.findByCodeIgnoreCase(code)
                        .orElseThrow(() -> new NotFoundException("LocalGovernment not found"));
                AdministrativeAreaResponseDto<LocalGovernmentDTO> result = new AdministrativeAreaResponseDto<>(
                        convertLocalGovernmentDTO(localGovernment));

                // Cache the result (1 hour TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, result, 1, TimeUnit.HOURS);
                }

                return result;
            }
            case COUNTY -> {
                // Check cache with specific type
                ParameterizedTypeReference<AdministrativeAreaResponseDto<CountyDTO>> typeRef = new ParameterizedTypeReference<AdministrativeAreaResponseDto<CountyDTO>>() {
                };
                AdministrativeAreaResponseDto<CountyDTO> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                County county = countyRepository.findByCodeIgnoreCase(code)
                        .orElseThrow(() -> new NotFoundException("County not found"));
                AdministrativeAreaResponseDto<CountyDTO> result = new AdministrativeAreaResponseDto<>(
                        convertCountyDTO(county));

                // Cache the result (1 hour TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, result, 1, TimeUnit.HOURS);
                }

                return result;
            }
            case SUBCOUNTY -> {
                // Check cache with specific type
                ParameterizedTypeReference<AdministrativeAreaResponseDto<SubCountyDTO>> typeRef = new ParameterizedTypeReference<AdministrativeAreaResponseDto<SubCountyDTO>>() {
                };
                AdministrativeAreaResponseDto<SubCountyDTO> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                SubCounty subCounty = subCountyRepository.findByCodeIgnoreCase(code)
                        .orElseThrow(() -> new NotFoundException("SubCounty not found"));
                AdministrativeAreaResponseDto<SubCountyDTO> result = new AdministrativeAreaResponseDto<>(
                        convertSubCountyDTO(subCounty));

                // Cache the result (1 hour TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, result, 1, TimeUnit.HOURS);
                }

                return result;
            }
            case PARISH -> {
                // Check cache with specific type
                ParameterizedTypeReference<AdministrativeAreaResponseDto<ParishDTO>> typeRef = new ParameterizedTypeReference<AdministrativeAreaResponseDto<ParishDTO>>() {
                };
                AdministrativeAreaResponseDto<ParishDTO> cached = cacheHelper
                        .get(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, typeRef);

                if (cached != null) {
                    return cached;
                }

                // Cache miss - fetch from database
                Parish parish = parishRepository.findByCodeIgnoreCase(code)
                        .orElseThrow(() -> new NotFoundException("Parish not found"));
                AdministrativeAreaResponseDto<ParishDTO> result = new AdministrativeAreaResponseDto<>(
                        convertParishDTO(parish));

                // Cache the result (1 hour TTL)
                if (result.isStatus()) {
                    cacheHelper.put(CacheValueKeyConfig.ADMINISTRATIVE_AREAS, cacheKey, result, 1, TimeUnit.HOURS);
                }

                return result;
            }
            default ->
                throw new MissingDataException("Unsupported Administrative Area Type: " + administrativeAreaType);
        }
    }

    @Override
    public AdministrativeAreaResponseDto<String> upload(List<AdministrativeAreaExcelDTO> dtoList) {
        // Input validation
        if (dtoList == null || dtoList.isEmpty()) {
            throw new MissingDataException("Upload list cannot be null or empty");
        }

        // Start async upload - return immediately, process in background
        // Flow: Regions → Sub-Regions → Local Governments → Counties → Sub-Counties →
        // Parishes
        // Each level ensures parent entities are saved before processing children
        // Entire flow is wrapped in @Transactional for atomicity
        uploadAsync(dtoList);

        // Return immediately - upload continues in background
        return new AdministrativeAreaResponseDto<>(
                "Upload started for " + dtoList.size() + " administrative area(s). Processing in background.");
    }

    private void uploadParishes(List<AdministrativeAreaExcelDTO> dtoList) {
        List<Parish> dbParishes = parishRepository.findAll();

        Set<UParish> newParishSet = dtoList.parallelStream().filter(dto -> dbParishes.stream().noneMatch(dbParish -> {
            SubCounty subCounty = dbParish.getSubCounty();
            County county = subCounty.getCounty();
            LocalGovernment localGovernment = county.getLocalGovernment();
            SubRegion subRegion = localGovernment.getSubRegion();
            Region region = subRegion.getRegion();
            return (dbParish.getName().equalsIgnoreCase(dto.getParish())
                    && subCounty.getName().equalsIgnoreCase(dto.getSubCounty())
                    && county.getName().equalsIgnoreCase(dto.getCounty())
                    && localGovernment.getName().equalsIgnoreCase(dto.getLocalGovernment())
                    && subRegion.getName().equalsIgnoreCase(dto.getSubRegion())
                    && region.getName().equalsIgnoreCase(dto.getRegion()));
        })).map(dto -> new UParish(dto.getParish(), dto.getDbSubCounty())).collect(Collectors.toSet());

        if (newParishSet.size() > 0) {
            List<Parish> newParishes = newParishSet.stream().map(UP -> {
                Parish parish = new Parish();
                parish.setCode(generateCode(AdministrativeAreaType.PARISH));
                parish.setName(UP.name());
                parish.setSubCounty(UP.subCounty());
                return parish;
            })
                    .toList();
            parishRepository.saveAll(newParishes);
        }

    }

    private void uploadSubCounty(List<AdministrativeAreaExcelDTO> dtoList) {
        List<SubCounty> dbSubCounties = subCountyRepository.findAll();

        Set<USubCounty> newSubCountySet = dtoList.parallelStream()
                .filter(dto -> dbSubCounties.stream().noneMatch(dbSubCounty -> {
                    County county = dbSubCounty.getCounty();
                    LocalGovernment localGovernment = county.getLocalGovernment();
                    SubRegion subRegion = localGovernment.getSubRegion();
                    Region region = subRegion.getRegion();
                    return (dbSubCounty.getName().equalsIgnoreCase(dto.getSubCounty())
                            && county.getName().equalsIgnoreCase(dto.getCounty())
                            && localGovernment.getName().equalsIgnoreCase(dto.getLocalGovernment())
                            && subRegion.getName().equalsIgnoreCase(dto.getSubRegion())
                            && region.getName().equalsIgnoreCase(dto.getRegion()));
                })).map(dto -> new USubCounty(dto.getSubCounty(), dto.getDbCounty())).collect(Collectors.toSet());

        List<SubCounty> dbSubCounties2;
        if (newSubCountySet.size() > 0) {
            List<SubCounty> newSubCounties = newSubCountySet.stream().map(dto -> {
                SubCounty subCounty = new SubCounty();
                subCounty.setCode(generateCode(AdministrativeAreaType.SUBCOUNTY));
                subCounty.setName(dto.name());
                subCounty.setCounty(dto.county());
                return subCounty;
            }).toList();

            subCountyRepository.saveAll(newSubCounties);
            dbSubCounties2 = subCountyRepository.findAll();
        } else {
            dbSubCounties2 = dbSubCounties;
        }

        // ADDING Subcounty TO LIST
        List<SubCounty> finalDbSubCounties = dbSubCounties2;
        List<AdministrativeAreaExcelDTO> newDtos = dtoList.parallelStream()
                .flatMap(oldDto -> finalDbSubCounties.stream()
                        .filter(dbSubCounty -> {
                            County county = dbSubCounty.getCounty();
                            LocalGovernment localGovernment = county.getLocalGovernment();
                            SubRegion subRegion = localGovernment.getSubRegion();
                            Region region = subRegion.getRegion();
                            return (dbSubCounty.getName().equalsIgnoreCase(oldDto.getSubCounty())
                                    && county.getName().equalsIgnoreCase(oldDto.getCounty())
                                    && localGovernment.getName().equalsIgnoreCase(oldDto.getLocalGovernment())
                                    && subRegion.getName().equalsIgnoreCase(oldDto.getSubRegion())
                                    && region.getName().equalsIgnoreCase(oldDto.getRegion()));
                        })
                        .map(subCounty -> {
                            oldDto.setDbSubCounty(subCounty);
                            return oldDto;
                        }))
                .toList();

        /// UPLOAD Parishes
        uploadParishes(newDtos);

    }

    private void uploadCounty(List<AdministrativeAreaExcelDTO> dtoList) {
        List<County> dbCounties = countyRepository.findAll();

        Set<UCounty> newCountSet = dtoList.parallelStream()
                .filter(dto -> dbCounties.stream().parallel().noneMatch(dbCounty -> {
                    LocalGovernment localGovernment = dbCounty.getLocalGovernment();
                    SubRegion subRegion = localGovernment.getSubRegion();
                    Region region = subRegion.getRegion();
                    return (dbCounty.getName().equalsIgnoreCase(dto.getCounty())
                            && localGovernment.getName().equalsIgnoreCase(dto.getLocalGovernment())
                            && subRegion.getName().equalsIgnoreCase(dto.getSubRegion())
                            && region.getName().equalsIgnoreCase(dto.getRegion()));
                })).map(dto -> new UCounty(dto.getCounty(), dto.getDbLocalGovernment())).collect(Collectors.toSet());

        List<County> dbCounties2;
        if (newCountSet.size() > 0) {
            List<County> newCounties = newCountSet.stream().map(UC -> {
                County county = new County();
                county.setCode(generateCode(AdministrativeAreaType.COUNTY));
                county.setName(UC.name());
                county.setLocalGovernment(UC.localGovernment());
                return county;
            }).toList();
            countyRepository.saveAll(newCounties);
            dbCounties2 = countyRepository.findAll();
        } else {
            dbCounties2 = dbCounties;
        }

        // ADDING Count TO LIST

        List<AdministrativeAreaExcelDTO> newDtos = dtoList.parallelStream()
                .flatMap(oldDto -> dbCounties2.stream()
                        .filter(dbCounty -> {
                            LocalGovernment localGovernment = dbCounty.getLocalGovernment();
                            SubRegion subRegion = localGovernment.getSubRegion();
                            Region region = subRegion.getRegion();
                            return (dbCounty.getName().equalsIgnoreCase(oldDto.getCounty())
                                    && localGovernment.getName().equalsIgnoreCase(oldDto.getLocalGovernment())
                                    && subRegion.getName().equalsIgnoreCase(oldDto.getSubRegion())
                                    && region.getName().equalsIgnoreCase(oldDto.getRegion()));
                        })
                        .map(county -> {
                            oldDto.setDbCounty(county);
                            return oldDto;
                        }))
                .toList();

        /// UPLOAD SUB COUNTYe());
        uploadSubCounty(newDtos);

    }

    private void uploadLocalGovernment(List<AdministrativeAreaExcelDTO> dtoList) {
        List<LocalGovernment> dbLocalGovernments = localGovernmentRepository.findAll();

        Set<ULocalGovernment> newLocalGovernmentSet = dtoList.parallelStream()
                .filter(dto -> dbLocalGovernments.stream().noneMatch(dbLocalGovernment -> {
                    SubRegion subRegion = dto.getDbSubRegion();
                    Region region = dto.getDbRegion();
                    return (dbLocalGovernment.getName().equalsIgnoreCase(dto.getLocalGovernment())
                            && subRegion.getName().equalsIgnoreCase(dto.getSubRegion())
                            && region.getName().equalsIgnoreCase(dto.getRegion()));
                })).map(dto -> new ULocalGovernment(dto.getLocalGovernment(), dto.getDbSubRegion()))
                .collect(Collectors.toSet());

        List<LocalGovernment> dbLocalGovernments2;
        if (newLocalGovernmentSet.size() > 0) {
            List<LocalGovernment> newLocalGovernments = newLocalGovernmentSet.stream().map(uL -> {
                LocalGovernment localGovernment = new LocalGovernment();
                localGovernment.setCode(generateCode(AdministrativeAreaType.LOCALGOVERNMENT));
                localGovernment.setName(uL.name());
                localGovernment.setSubRegion(uL.subRegion());
                return localGovernment;
            }).toList();
            localGovernmentRepository.saveAll(newLocalGovernments);
            dbLocalGovernments2 = localGovernmentRepository.findAll();
        } else {
            dbLocalGovernments2 = dbLocalGovernments;
        }

        // ADDING LOCAL GOVERNMENT TO LIST
        List<AdministrativeAreaExcelDTO> newDtos = dtoList.parallelStream()
                .flatMap(oldDto -> dbLocalGovernments2.stream()
                        .filter(dbLocalGovernment -> {
                            SubRegion subRegion = dbLocalGovernment.getSubRegion();
                            Region region = subRegion.getRegion();
                            return (dbLocalGovernment.getName().equalsIgnoreCase(oldDto.getLocalGovernment())
                                    && subRegion.getName().equalsIgnoreCase(oldDto.getSubRegion())
                                    && region.getName().equalsIgnoreCase(oldDto.getRegion()));
                        })
                        .map(localGovernment -> {
                            oldDto.setDbLocalGovernment(localGovernment);
                            return oldDto;
                        }))
                .toList();

        /// UPLOAD COUNTY
        uploadCounty(newDtos);

    }

    /**
     * Step 2: Process Sub-Regions (parent: Region)
     * 1. Extract all unique sub-regions from Excel DTOs (using DB region
     * references)
     * 2. Check against existing DB sub-regions (name + parent region match)
     * 3. Filter out existing, save new ones
     * 4. Fetch updated complete list from DB
     * 5. Update DTOs with DB sub-region references
     * 6. Proceed to Local Governments (ensuring parent sub-regions exist)
     */
    private void uploadSubRegions(List<AdministrativeAreaExcelDTO> dtoList) {
        // Step 1: Get all existing sub-regions from database
        List<SubRegion> dbSubRegions = subRegionRepository.findAll();

        // Step 2: Extract unique new sub-regions (exclude existing ones)
        // Uses dto.getDbRegion() which was set in uploadRegions()
        Set<USubRegion> newSubRegionSet = dtoList.stream()
                .filter(dto -> dbSubRegions.stream().noneMatch(dbSubRegion -> {
                    Region region = dto.getDbRegion();
                    return (dbSubRegion.getName().equalsIgnoreCase(dto.getSubRegion())
                            && region.getName().equalsIgnoreCase(dto.getRegion()));
                }))
                .map(dto -> new USubRegion(dto.getSubRegion(), dto.getDbRegion()))
                .collect(Collectors.toSet());

        // Step 3: Save new sub-regions and fetch updated complete list
        List<SubRegion> dbSubRegions2;
        if (newSubRegionSet.size() > 0) {
            List<SubRegion> newSubRegions = newSubRegionSet.stream().map(uSubRegion -> {
                SubRegion subRegion = new SubRegion();
                subRegion.setCode(generateCode(AdministrativeAreaType.SUBREGION));
                subRegion.setName(uSubRegion.name());
                subRegion.setRegion(uSubRegion.region()); // Parent region reference
                return subRegion;
            }).toList();
            subRegionRepository.saveAll(newSubRegions);
            dbSubRegions2 = subRegionRepository.findAll(); // Fetch updated list
        } else {
            dbSubRegions2 = dbSubRegions; // No new sub-regions, use existing list
        }

        // Step 4: Update DTOs with DB sub-region references (for child processing)
        List<AdministrativeAreaExcelDTO> newDtos = dtoList.parallelStream()
                .flatMap(oldDto -> dbSubRegions2.stream()
                        .filter(dbSubRegion -> {
                            Region region = dbSubRegion.getRegion();
                            return (dbSubRegion.getName().equalsIgnoreCase(oldDto.getSubRegion())
                                    && region.getName().equalsIgnoreCase(oldDto.getRegion()));
                        })
                        .map(subRegion -> {
                            oldDto.setDbSubRegion(subRegion);
                            return oldDto;
                        }))
                .toList();

        // Step 5: Proceed to Local Governments (parent sub-regions now guaranteed to
        // exist)
        uploadLocalGovernment(newDtos);
    }

    /**
     * Async method to process upload in background.
     * Uses Spring's @Async for proper thread pool management.
     * Entire hierarchical upload is wrapped in @Transactional for atomicity.
     * Flow: Regions → Sub-Regions → Local Governments → Counties → Sub-Counties →
     * Parishes
     * If any level fails, entire operation rolls back to maintain data integrity.
     * 
     * The hierarchical chain continues automatically:
     * uploadRegions() → uploadSubRegions() → uploadLocalGovernment()
     * → uploadCounty() → uploadSubCounty() → uploadParishes()
     */
    @Async
    @Transactional
    public void uploadAsync(List<AdministrativeAreaExcelDTO> dtoList) {
        try {
            log.info("Starting hierarchical upload of {} administrative area(s)", dtoList.size());

            // Execute entire hierarchical upload in a single transaction
            // If any level fails, entire operation rolls back
            // Chain: Regions → Sub-Regions → Local Governments → Counties → Sub-Counties →
            // Parishes
            uploadRegions(dtoList);

            // Evict service-level cache after bulk upload completes (only if transaction
            // succeeded)
            cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS);
            cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER);

            log.info("Successfully completed hierarchical upload of {} administrative area(s)", dtoList.size());
        } catch (Exception e) {
            log.error("Failed to upload administrative areas: {}", e.getMessage(), e);
            // Transaction will automatically rollback on exception
            // Optionally: notify user via email/notification system, update status in DB,
            // etc.
            throw e; // Re-throw to trigger rollback
        }
    }

    /**
     * Step 1: Process Regions (top level - no parent dependencies)
     * 1. Extract all unique regions from Excel DTOs
     * 2. Check against existing DB regions (case-insensitive name match)
     * 3. Filter out existing regions, keep only new ones
     * 4. Save new regions to database
     * 5. Fetch updated complete list from DB (existing + newly saved)
     * 6. Update DTOs with DB region references
     * 7. Proceed to Sub-Regions (ensuring parent regions exist)
     */
    private void uploadRegions(List<AdministrativeAreaExcelDTO> dtoList) {
        // Step 1: Get all existing regions from database
        List<Region> dbRegions = regionRepository.findAll();

        // Step 2: Extract unique new regions from Excel (exclude existing ones)
        List<Region> newRegions = dtoList.parallelStream()
                .filter(dto -> dbRegions.stream()
                        .noneMatch(dbRegion -> dbRegion.getName().equalsIgnoreCase(dto.getRegion())))
                .filter(distinctByKey(AdministrativeAreaExcelDTO::getRegion))
                .map(dto -> {
                    Region region = new Region();
                    region.setCode(generateCode(AdministrativeAreaType.REGION));
                    region.setName(dto.getRegion());
                    return region;
                })
                .toList();

        // Step 3: Save new regions and fetch updated complete list
        List<Region> dbRegions2;
        if (newRegions.size() > 0) {
            regionRepository.saveAll(newRegions);
            dbRegions2 = regionRepository.findAll(); // Fetch updated list (existing + new)
        } else {
            dbRegions2 = dbRegions; // No new regions, use existing list
        }

        // Step 4: Update DTOs with DB region references (for child processing)
        List<AdministrativeAreaExcelDTO> newDtos = dtoList.parallelStream()
                .flatMap(oldDto -> dbRegions2.stream()
                        .filter(region -> region.getName().equalsIgnoreCase(oldDto.getRegion()))
                        .map(region -> {
                            oldDto.setDbRegion(region);
                            return oldDto;
                        }))
                .toList();

        // Step 5: Proceed to Sub-Regions (parent regions now guaranteed to exist)
        uploadSubRegions(newDtos);
    }

    public static <T> Predicate<T> distinctByKey(
            Function<? super T, ?> keyExtractor) {

        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    @Override
    public AdministrativeAreaResponseDto<String> updateOne(Map<String, String> queryMap,
            UpdateAdministrativeAreaDTO dto) {
        String type = queryMap.get("type");

        Optional<AdministrativeAreaType> optionalAdministrativeAreaType = AdministrativeAreaType.fromStr(type);
        if (optionalAdministrativeAreaType.isEmpty()) {
            throw new MissingDataException("Missing Administrative Area Type");
        }

        AdministrativeAreaType administrativeAreaType = optionalAdministrativeAreaType.get();

        AdministrativeAreaResponseDto<String> result = switch (administrativeAreaType) {
            case REGION -> {
                Region region = regionRepository.findByCodeIgnoreCase(dto.getCode())
                        .orElseThrow(() -> new InvalidException("Invalid PartOfCode"));
                if (notNullEmpty(dto.getName())) {
                    region.setName(dto.getName());
                }

                if (nullEmpty(dto.getLongitude())) {
                    region.setLatitude(Double.valueOf(dto.getLatitude()));
                }

                if (nullEmpty(dto.getLongitude())) {
                    region.setLatitude(Double.valueOf(dto.getLongitude()));
                }

                if (notNullEmpty(dto.getDescription())) {
                    region.setDescription(dto.getDescription());
                }

                regionRepository.save(region);
                yield new AdministrativeAreaResponseDto<>("SUCCESS");
            }
            case SUBREGION -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode");
                }

                Region region = regionRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                        .orElseThrow(() -> new InvalidException("Invalid PartOfCode"));
                SubRegion subRegion = subRegionRepository.findByCodeIgnoreCase(dto.getCode())
                        .orElseThrow(() -> new NotFoundException("Administrative Area NotFound"));

                if (notNullEmpty(dto.getName())) {
                    subRegion.setName(dto.getName());
                }

                if (nullEmpty(dto.getLongitude())) {
                    subRegion.setLatitude(Double.valueOf(dto.getLatitude()));
                }

                if (nullEmpty(dto.getLongitude())) {
                    subRegion.setLatitude(Double.valueOf(dto.getLongitude()));
                }

                if (notNullEmpty(dto.getDescription())) {
                    subRegion.setDescription(dto.getDescription());
                }

                subRegion.setRegion(region);

                subRegionRepository.save(subRegion);
                yield new AdministrativeAreaResponseDto<>("SUCCESS");
            }
            case LOCALGOVERNMENT -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode");
                }

                SubRegion subRegion = subRegionRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                        .orElseThrow(() -> new InvalidException("Invalid PartOfCode"));
                LocalGovernment localGovernment = localGovernmentRepository.findByCodeIgnoreCase(dto.getCode())
                        .orElseThrow(() -> new NotFoundException("Administrative Area NotFound"));

                if (notNullEmpty(dto.getName())) {
                    localGovernment.setName(dto.getName());
                }

                if (nullEmpty(dto.getLongitude())) {
                    localGovernment.setLatitude(Double.valueOf(dto.getLatitude()));
                }

                if (nullEmpty(dto.getLongitude())) {
                    localGovernment.setLatitude(Double.valueOf(dto.getLongitude()));
                }

                if (notNullEmpty(dto.getDescription())) {
                    localGovernment.setDescription(dto.getDescription());
                }

                localGovernment.setSubRegion(subRegion);

                localGovernmentRepository.save(localGovernment);
                yield new AdministrativeAreaResponseDto<>("SUCCESS");
            }
            case COUNTY -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode");
                }

                LocalGovernment localGovernment = localGovernmentRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                        .orElseThrow(() -> new InvalidException("Invalid PartOfCode"));
                County county = countyRepository.findByCodeIgnoreCase(dto.getCode())
                        .orElseThrow(() -> new NotFoundException("Administrative Area NotFound"));

                if (notNullEmpty(dto.getName())) {
                    county.setName(dto.getName());
                }

                if (nullEmpty(dto.getLongitude())) {
                    county.setLatitude(Double.valueOf(dto.getLatitude()));
                }

                if (nullEmpty(dto.getLongitude())) {
                    county.setLatitude(Double.valueOf(dto.getLongitude()));
                }

                if (notNullEmpty(dto.getDescription())) {
                    county.setDescription(dto.getDescription());
                }

                county.setLocalGovernment(localGovernment);

                countyRepository.save(county);
                yield new AdministrativeAreaResponseDto<>("SUCCESS");
            }
            case SUBCOUNTY -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode");
                }

                County county = countyRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                        .orElseThrow(() -> new InvalidException("Invalid PartOfCode"));
                SubCounty subCounty = subCountyRepository.findByCodeIgnoreCase(dto.getCode())
                        .orElseThrow(() -> new NotFoundException("Administrative Area NotFound"));

                if (notNullEmpty(dto.getName())) {
                    subCounty.setName(dto.getName());
                }

                if (nullEmpty(dto.getLongitude())) {
                    subCounty.setLatitude(Double.valueOf(dto.getLatitude()));
                }

                if (nullEmpty(dto.getLongitude())) {
                    subCounty.setLatitude(Double.valueOf(dto.getLongitude()));
                }

                if (notNullEmpty(dto.getDescription())) {
                    subCounty.setDescription(dto.getDescription());
                }

                subCounty.setCounty(county);

                subCountyRepository.save(subCounty);
                yield new AdministrativeAreaResponseDto<>("SUCCESS");
            }
            case PARISH -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode");
                }

                SubCounty subCounty = subCountyRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                        .orElseThrow(() -> new InvalidException("Invalid PartOfCode"));
                Parish parish = parishRepository.findByCodeIgnoreCase(dto.getCode())
                        .orElseThrow(() -> new NotFoundException("Administrative Area NotFound"));

                if (notNullEmpty(dto.getName())) {
                    parish.setName(dto.getName());
                }

                if (nullEmpty(dto.getLongitude())) {
                    parish.setLatitude(Double.valueOf(dto.getLatitude()));
                }

                if (nullEmpty(dto.getLongitude())) {
                    parish.setLatitude(Double.valueOf(dto.getLongitude()));
                }

                if (notNullEmpty(dto.getDescription())) {
                    parish.setDescription(dto.getDescription());
                }

                parish.setSubCounty(subCounty);

                parishRepository.save(parish);
                yield new AdministrativeAreaResponseDto<>("SUCCESS");
            }
        };

        // Evict service-level cache after update operation
        cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS);
        cacheHelper.evictAll(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER);

        return result;
    }

    private ParishDTO convertParishDTO(Parish parish) {
        ParishDTO dto = new ParishDTO();
        dto.setCode(parish.getCode());
        dto.setName(parish.getName());
        dto.setLatitude(parish.getLatitude() != null ? String.valueOf(parish.getLatitude()) : "");
        dto.setLongitude(parish.getLongitude() != null ? String.valueOf(parish.getLongitude()) : "");

        dto.setSubCounty(convertSubCountyDTO(parish.getSubCounty()));

        return dto;
    }

    private SubCountyDTO convertSubCountyDTO(SubCounty subCounty) {
        SubCountyDTO dto = new SubCountyDTO();
        dto.setCode(subCounty.getCode());
        dto.setName(subCounty.getName());
        dto.setLatitude(subCounty.getLatitude() != null ? String.valueOf(subCounty.getLatitude()) : "");
        dto.setLongitude(subCounty.getLongitude() != null ? String.valueOf(subCounty.getLongitude()) : "");

        dto.setCounty(convertCountyDTO(subCounty.getCounty()));

        return dto;
    }

    private CountyDTO convertCountyDTO(County county) {
        CountyDTO dto = new CountyDTO();
        dto.setCode(county.getCode());
        dto.setName(county.getName());
        dto.setLatitude(county.getLatitude() != null ? String.valueOf(county.getLatitude()) : "");
        dto.setLongitude(county.getLongitude() != null ? String.valueOf(county.getLongitude()) : "");

        dto.setLocalGovernment(convertLocalGovernmentDTO(county.getLocalGovernment()));

        return dto;
    }

    private LocalGovernmentDTO convertLocalGovernmentDTO(LocalGovernment localGovernment) {
        LocalGovernmentDTO dto = new LocalGovernmentDTO();
        dto.setCode(localGovernment.getCode());
        dto.setName(localGovernment.getName());
        dto.setLatitude(localGovernment.getLatitude() != null ? String.valueOf(localGovernment.getLatitude()) : "");
        dto.setLongitude(localGovernment.getLongitude() != null ? String.valueOf(localGovernment.getLongitude()) : "");

        dto.setSubRegion(convertSubRegionDTO(localGovernment.getSubRegion()));

        return dto;
    }

    private static SubRegionDTO convertSubRegionDTO(SubRegion subRegion) {
        SubRegionDTO dto = new SubRegionDTO();
        dto.setCode(subRegion.getCode());
        dto.setName(subRegion.getName());
        dto.setLatitude(subRegion.getLatitude() != null ? String.valueOf(subRegion.getLatitude()) : "");
        dto.setLongitude(subRegion.getLongitude() != null ? String.valueOf(subRegion.getLongitude()) : "");

        dto.setRegion(convertRegionDTO(subRegion.getRegion()));

        return dto;
    }

    private static RegionDTO convertRegionDTO(Region region) {
        RegionDTO dto = new RegionDTO();
        dto.setCode(region.getCode());
        dto.setName(region.getName());
        dto.setLongitude(region.getLongitude() != null ? String.valueOf(region.getLongitude()) : "");
        dto.setLatitude(region.getLatitude() != null ? String.valueOf(region.getLatitude()) : "");
        return dto;
    }

}
