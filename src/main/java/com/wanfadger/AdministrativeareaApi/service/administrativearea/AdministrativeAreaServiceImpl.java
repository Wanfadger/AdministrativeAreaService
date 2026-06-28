package com.wanfadger.AdministrativeareaApi.service.administrativearea;

import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.AlreadyExistsException;
import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.MissingDataException;
import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.NotFoundException;
import com.wanfadger.AdministrativeareaApi.beanConfig.CacheValueKeyConfig;
import com.wanfadger.AdministrativeareaApi.dto.*;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.uniqueDtos.*;
import com.wanfadger.AdministrativeareaApi.entity.*;
import com.wanfadger.AdministrativeareaApi.repository.*;
import com.wanfadger.AdministrativeareaApi.repository.specification.MatchType;
import com.wanfadger.AdministrativeareaApi.repository.specification.SpecificationBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Single dispatched-resource service for the administrative-area hierarchy
 * (Region › Sub-Region › Local Government › County › Sub-County › Parish).
 *
 * <p>Data access goes directly through the six JPA repositories; caching is handled by
 * Spring's cache abstraction (Redis) via {@code @Cacheable}/{@code @CacheEvict} instead of
 * the previous manual helper. Reads are cached per request query-map (stable key); every
 * write evicts all three administrative-area cache regions.
 */
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
    private final CacheManager cacheManager;

    /** Query keys consumed directly by the search method, excluded from generic column filters. */
    private static final Set<String> HANDLED_SEARCH_KEYS =
            Set.of("type", "partOf", "partOfCode", "detailed");

    private boolean notNullEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    private boolean nullEmpty(String value) {
        return value == null || value.isEmpty();
    }

    private String generateCode(AdministrativeAreaType administrativeAreaType) {
        return switch (administrativeAreaType) {
            case REGION -> {
                String code;
                do { code = UUID.randomUUID().toString(); } while (regionRepository.findByCodeIgnoreCase(code).isPresent());
                yield code;
            }
            case SUBREGION -> {
                String code;
                do { code = UUID.randomUUID().toString(); } while (subRegionRepository.findByCodeIgnoreCase(code).isPresent());
                yield code;
            }
            case LOCALGOVERNMENT -> {
                String code;
                do { code = UUID.randomUUID().toString(); } while (localGovernmentRepository.findByCodeIgnoreCase(code).isPresent());
                yield code;
            }
            case COUNTY -> {
                String code;
                do { code = UUID.randomUUID().toString(); } while (countyRepository.findByCodeIgnoreCase(code).isPresent());
                yield code;
            }
            case SUBCOUNTY -> {
                String code;
                do { code = UUID.randomUUID().toString(); } while (subCountyRepository.findByCodeIgnoreCase(code).isPresent());
                yield code;
            }
            case PARISH -> {
                String code;
                do { code = UUID.randomUUID().toString(); } while (parishRepository.findByCodeIgnoreCase(code).isPresent());
                yield code;
            }
        };
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS, allEntries = true),
            @CacheEvict(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, allEntries = true),
            @CacheEvict(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS_SEARCH, allEntries = true)
    })
    public ResponseEntity<ResponseDTO<String>> newOne(Map<String, String> queryMap, NewAdministrativeAreaDTO dto) {
        AdministrativeAreaType administrativeAreaType = AdministrativeAreaType
                .fromStr(queryMap.get("type")).orElseThrow(() -> new MissingDataException("Missing Administrative Area Type"));

        return switch (administrativeAreaType) {
            case REGION -> {
                if (regionRepository.findByNameIgnoreCase(dto.getName()).isPresent()) {
                    throw new AlreadyExistsException("Administrative Area Already Exists");
                }
                Region region = convertDtoRegion(dto, administrativeAreaType);
                regionRepository.save(region);
                yield new ResponseEntity<>(new ResponseDTO<>(region.getCode(), "successfully created a region"), HttpStatus.CREATED);
            }
            case SUBREGION -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode(region) for the sub region");
                }
                Region region = regionRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                        .orElseThrow(() -> new InvalidException("Invalid PartOfCode: " + dto.getPartOfCode()));
                if (subRegionRepository.findByNameIgnoreCaseAndRegion_Code(dto.getName(), dto.getPartOfCode()).isPresent()) {
                    throw new AlreadyExistsException("Sub region Already Exists in the region");
                }
                SubRegion subRegion = convertDtoSubRegion(dto, administrativeAreaType);
                subRegion.setRegion(region);
                subRegionRepository.save(subRegion);
                yield new ResponseEntity<>(new ResponseDTO<>(subRegion.getCode(), "success"), HttpStatus.CREATED);
            }
            case LOCALGOVERNMENT -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode(sub region) for localgovernment");
                }
                SubRegion subRegion = subRegionRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                        .orElseThrow(() -> new InvalidException("Invalid PartOfCode: " + dto.getPartOfCode()));
                if (localGovernmentRepository.findByNameIgnoreCaseAndSubRegion_Code(dto.getName(), dto.getPartOfCode()).isPresent()) {
                    throw new AlreadyExistsException("Local Government Already Exists in the sub region");
                }
                LocalGovernment localGovernment = convertDtoLocalGovernment(dto, administrativeAreaType);
                localGovernment.setSubRegion(subRegion);
                localGovernmentRepository.save(localGovernment);
                yield new ResponseEntity<>(new ResponseDTO<>(localGovernment.getCode(), "success"), HttpStatus.CREATED);
            }
            case COUNTY -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode(local government) for county");
                }
                LocalGovernment localGovernment = localGovernmentRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                        .orElseThrow(() -> new InvalidException("Invalid PartOfCode: " + dto.getPartOfCode()));
                if (countyRepository.findByNameIgnoreCaseAndLocalGovernment_Code(dto.getName(), dto.getPartOfCode()).isPresent()) {
                    throw new AlreadyExistsException("County Already Exists in the local government");
                }
                County county = convertDtoCounty(dto, administrativeAreaType);
                county.setLocalGovernment(localGovernment);
                countyRepository.save(county);
                yield new ResponseEntity<>(new ResponseDTO<>(county.getCode(), "success"), HttpStatus.CREATED);
            }
            case SUBCOUNTY -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode(county) for sub county");
                }
                County county = countyRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                        .orElseThrow(() -> new InvalidException("Invalid PartOfCode"));
                if (subCountyRepository.findByNameIgnoreCaseAndCounty_Id(dto.getName(), dto.getPartOfCode()).isPresent()) {
                    throw new AlreadyExistsException("Sub County Already Exists in the county");
                }
                SubCounty subCounty = convertDtoSubCounty(dto, administrativeAreaType);
                subCounty.setCounty(county);
                subCountyRepository.save(subCounty);
                yield new ResponseEntity<>(new ResponseDTO<>(subCounty.getCode(), "success"), HttpStatus.CREATED);
            }
            case PARISH -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode(sub county) for parish");
                }
                SubCounty subCounty = subCountyRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                        .orElseThrow(() -> new InvalidException("Invalid PartOfCode: " + dto.getPartOfCode()));
                if (parishRepository.findByNameIgnoreCaseAndSubCounty_Code(dto.getName(), dto.getPartOfCode()).isPresent()) {
                    throw new AlreadyExistsException("Parish Already Exists in the sub county");
                }
                Parish parish = convertDtoParish(dto, administrativeAreaType);
                parish.setSubCounty(subCounty);
                parishRepository.save(parish);
                yield new ResponseEntity<>(new ResponseDTO<>(parish.getCode(), "success"), HttpStatus.CREATED);
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

    private LocalGovernment convertDtoLocalGovernment(NewAdministrativeAreaDTO dto, AdministrativeAreaType administrativeAreaType) {
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
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS, allEntries = true),
            @CacheEvict(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, allEntries = true),
            @CacheEvict(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS_SEARCH, allEntries = true)
    })
    public ResponseEntity<ResponseDTO<String>> newList(Map<String, String> queryMap, List<NewAdministrativeAreaDTO> dtos) {
        AdministrativeAreaType administrativeAreaType = AdministrativeAreaType.fromStr(queryMap.get("type"))
                .orElseThrow(() -> new MissingDataException("Missing Administrative Area Type"));

        return switch (administrativeAreaType) {
            case REGION -> {
                List<Region> regions = dtos.parallelStream()
                        .filter(dto -> regionRepository.findByNameIgnoreCase(dto.getName()).isEmpty())
                        .map(dto -> convertDtoRegion(dto, administrativeAreaType)).toList();
                regionRepository.saveAll(regions);
                yield new ResponseEntity<>(new ResponseDTO<>("success", "successfully added " + regions.size() + " administrative areas"), HttpStatus.CREATED);
            }
            case SUBREGION -> {
                if (dtos.parallelStream().anyMatch(dto -> !notNullEmpty(dto.getPartOfCode()))) {
                    throw new MissingDataException("Found Administrative Area without PartOfCoce");
                }
                List<SubRegion> subRegions = dtos.parallelStream()
                        .filter(dto -> subRegionRepository.findByNameIgnoreCaseAndRegion_Code(dto.getName(), dto.getPartOfCode()).isEmpty())
                        .map(dto -> convertDtoSubRegion(dto, administrativeAreaType)).toList();
                subRegionRepository.saveAll(subRegions);
                yield new ResponseEntity<>(new ResponseDTO<>("success", "successfully added " + subRegions.size() + " administrative areas"), HttpStatus.CREATED);
            }
            case LOCALGOVERNMENT -> {
                if (dtos.parallelStream().anyMatch(dto -> !notNullEmpty(dto.getPartOfCode()))) {
                    throw new MissingDataException("Found Administrative Area without PartOfCoce");
                }
                List<LocalGovernment> localGovernments = dtos.parallelStream()
                        .filter(dto -> localGovernmentRepository.findByNameIgnoreCaseAndSubRegion_Code(dto.getName(), dto.getPartOfCode()).isEmpty())
                        .map(dto -> convertDtoLocalGovernment(dto, administrativeAreaType)).toList();
                localGovernmentRepository.saveAll(localGovernments);
                yield new ResponseEntity<>(new ResponseDTO<>("success", "successfully added " + localGovernments.size() + " administrative areas"), HttpStatus.CREATED);
            }
            case COUNTY -> {
                if (dtos.parallelStream().anyMatch(dto -> !notNullEmpty(dto.getPartOfCode()))) {
                    throw new MissingDataException("Found Administrative Area without PartOfCoce");
                }
                List<County> counties = dtos.parallelStream()
                        .filter(dto -> countyRepository.findByNameIgnoreCaseAndLocalGovernment_Code(dto.getName(), dto.getPartOfCode()).isEmpty())
                        .map(dto -> convertDtoCounty(dto, administrativeAreaType)).toList();
                countyRepository.saveAll(counties);
                yield new ResponseEntity<>(new ResponseDTO<>("success", "successfully added " + counties.size() + " administrative areas"), HttpStatus.CREATED);
            }
            case SUBCOUNTY -> {
                if (dtos.parallelStream().anyMatch(dto -> !notNullEmpty(dto.getPartOfCode()))) {
                    throw new MissingDataException("Found Administrative Area without PartOfCoce");
                }
                List<SubCounty> subCounties = dtos.parallelStream()
                        .filter(dto -> subCountyRepository.findByNameIgnoreCaseAndCounty_Id(dto.getName(), dto.getPartOfCode()).isEmpty())
                        .map(dto -> convertDtoSubCounty(dto, administrativeAreaType)).toList();
                subCountyRepository.saveAll(subCounties);
                yield new ResponseEntity<>(new ResponseDTO<>("success", "successfully added " + subCounties.size() + " administrative areas"), HttpStatus.CREATED);
            }
            case PARISH -> {
                if (dtos.parallelStream().anyMatch(dto -> !notNullEmpty(dto.getPartOfCode()))) {
                    throw new MissingDataException("Found Administrative Area without PartOfCoce");
                }
                List<Parish> parishes = dtos.parallelStream()
                        .filter(dto -> parishRepository.findByNameIgnoreCaseAndSubCounty_Code(dto.getName(), dto.getPartOfCode()).isEmpty())
                        .map(dto -> convertDtoParish(dto, administrativeAreaType)).toList();
                parishRepository.saveAll(parishes);
                yield new ResponseEntity<>(new ResponseDTO<>("success", "successfully added " + parishes.size() + " administrative areas"), HttpStatus.CREATED);
            }
        };
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER,
            keyGenerator = "searchKeyGenerator", cacheManager = "weekCacheManager")
    public ResponseDTO<CodeNameDTO> filterOne(Map<String, String> queryMap) {
        String type = queryMap.get("type");
        String code = queryMap.get("code");

        if (!notNullEmpty(type)) {
            throw new MissingDataException("Missing Administrative Area Type");
        }
        AdministrativeAreaType administrativeAreaType = AdministrativeAreaType.fromStr(type)
                .orElseThrow(() -> new MissingDataException("Missing Administrative Area Type"));

        if (!notNullEmpty(code)) {
            throw new MissingDataException("Missing Administrative Area Code");
        }

        CodeNameDTO codeName = switch (administrativeAreaType) {
            case REGION -> {
                Region region = regionRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("Region not found"));
                yield new CodeNameDTO(region.getCode(), region.getName());
            }
            case SUBREGION -> {
                SubRegion subRegion = subRegionRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("SubRegion not found"));
                yield new CodeNameDTO(subRegion.getCode(), subRegion.getName());
            }
            case LOCALGOVERNMENT -> {
                LocalGovernment localGovernment = localGovernmentRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("LocalGovernment not found"));
                yield new CodeNameDTO(localGovernment.getCode(), localGovernment.getName());
            }
            case COUNTY -> {
                County county = countyRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("County not found"));
                yield new CodeNameDTO(county.getCode(), county.getName());
            }
            case SUBCOUNTY -> {
                SubCounty subCounty = subCountyRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("SubCounty not found"));
                yield new CodeNameDTO(subCounty.getCode(), subCounty.getName());
            }
            case PARISH -> {
                Parish parish = parishRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("Parish not found"));
                yield new CodeNameDTO(parish.getCode(), parish.getName());
            }
        };
        return new ResponseDTO<>(codeName);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER,
            keyGenerator = "searchKeyGenerator", cacheManager = "weekCacheManager")
    public ResponseDTO<List<CodeNameDTO>> filterList(Map<String, String> queryMap) {
        String type = queryMap.get("type");
        String partOf = queryMap.get("partOf");

        if (!notNullEmpty(type)) {
            throw new MissingDataException("Missing Administrative Area Type");
        }
        AdministrativeAreaType administrativeAreaType = AdministrativeAreaType.fromStr(type)
                .orElseThrow(() -> new MissingDataException("Missing Administrative Area Type"));

        List<CodeNameDTO> codeNameDtoList = switch (administrativeAreaType) {
            case REGION -> regionRepository.findAll().parallelStream()
                    .map(region -> new CodeNameDTO(region.getCode(), region.getName()))
                    .sorted(Comparator.comparing(CodeNameDTO::getCode)).toList();
            case SUBREGION -> {
                if (!notNullEmpty(partOf)) throw new MissingDataException("Missing Administrative Area partOf");
                yield subRegionRepository.findAllByRegion_Code(partOf).parallelStream()
                        .map(subRegion -> new CodeNameDTO(subRegion.getCode(), subRegion.getName()))
                        .sorted(Comparator.comparing(CodeNameDTO::getCode)).toList();
            }
            case LOCALGOVERNMENT -> {
                if (!notNullEmpty(partOf)) throw new MissingDataException("Missing Administrative Area partOf");
                yield localGovernmentRepository.findAllBySubRegion_Code(partOf).parallelStream()
                        .map(localGovernment -> new CodeNameDTO(localGovernment.getCode(), localGovernment.getName()))
                        .sorted(Comparator.comparing(CodeNameDTO::getCode)).toList();
            }
            case COUNTY -> {
                if (!notNullEmpty(partOf)) throw new MissingDataException("Missing Administrative Area partOf");
                yield countyRepository.findAllByLocalGovernment_Code(partOf).parallelStream()
                        .map(county -> new CodeNameDTO(county.getCode(), county.getName()))
                        .sorted(Comparator.comparing(CodeNameDTO::getCode)).toList();
            }
            case SUBCOUNTY -> {
                if (!notNullEmpty(partOf)) throw new MissingDataException("Missing Administrative Area partOf");
                yield subCountyRepository.findAllByCounty_Code(partOf).parallelStream()
                        .map(subCounty -> new CodeNameDTO(subCounty.getCode(), subCounty.getName()))
                        .sorted(Comparator.comparing(CodeNameDTO::getCode)).toList();
            }
            case PARISH -> {
                if (!notNullEmpty(partOf)) throw new MissingDataException("Missing Administrative Area partOf");
                yield parishRepository.findAllBySubCounty_Code(partOf).parallelStream()
                        .map(parish -> new CodeNameDTO(parish.getCode(), parish.getName()))
                        .sorted(Comparator.comparing(CodeNameDTO::getCode)).toList();
            }
        };
        return new ResponseDTO<>(codeNameDtoList);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER,
            keyGenerator = "searchKeyGenerator", cacheManager = "weekCacheManager")
    public ResponseDTO<List<CodeNameDTO>> getParishByPartOf(Map<String, String> queryMap) {
        String type = queryMap.get("type");
        String partOfCode = queryMap.get("partOfCode");

        if (!notNullEmpty(type)) {
            throw new MissingDataException("Missing Administrative Area Type");
        }
        AdministrativeAreaType administrativeAreaType = AdministrativeAreaType.fromStr(type)
                .orElseThrow(() -> new MissingDataException("Missing Administrative Area Type"));

        List<CodeNameDTO> codeNameDtoList = switch (administrativeAreaType) {
            case REGION -> {
                List<String> subRegionCodes = subRegionRepository.findAllByRegion_Code(partOfCode).parallelStream()
                        .map(SubRegion::getCode).distinct().toList();
                List<String> lgCodes = localGovernmentRepository.findAllBySubRegionCodes(subRegionCodes).parallelStream()
                        .map(LocalGovernment::getCode).distinct().toList();
                List<String> countyCodes = countyRepository.findAllByLocalGovernmentCodes(lgCodes).parallelStream()
                        .map(County::getCode).distinct().toList();
                List<String> subCounties = subCountyRepository.findAllByCountyCodes(countyCodes).parallelStream()
                        .map(SubCounty::getCode).distinct().toList();
                yield parishRepository.findAllBySubCountyCodes(subCounties).parallelStream()
                        .map(parish -> new CodeNameDTO(parish.getCode(), parish.getName())).distinct().toList();
            }
            case SUBREGION -> {
                if (!notNullEmpty(partOfCode)) throw new MissingDataException("Missing Administrative Area partOf");
                List<String> lgCodes = localGovernmentRepository.findAllBySubRegion_Code(partOfCode).parallelStream()
                        .map(LocalGovernment::getCode).distinct().toList();
                List<String> countyCodes = countyRepository.findAllByLocalGovernmentCodes(lgCodes).parallelStream()
                        .map(County::getCode).distinct().toList();
                List<String> subCounties = subCountyRepository.findAllByCountyCodes(countyCodes).parallelStream()
                        .map(SubCounty::getCode).distinct().toList();
                yield parishRepository.findAllBySubCountyCodes(subCounties).parallelStream()
                        .map(parish -> new CodeNameDTO(parish.getCode(), parish.getName()))
                        .sorted(Comparator.comparing(CodeNameDTO::getCode)).toList();
            }
            case LOCALGOVERNMENT -> {
                if (!notNullEmpty(partOfCode)) throw new MissingDataException("Missing Administrative Area partOf");
                List<String> countyCodes = countyRepository.findAllByLocalGovernment_Code(partOfCode).parallelStream()
                        .map(County::getCode).distinct().toList();
                List<String> subCountyCodes = subCountyRepository.findAllByCountyCodes(countyCodes).parallelStream()
                        .map(SubCounty::getCode).distinct().toList();
                yield parishRepository.findAllBySubCountyCodes(subCountyCodes).parallelStream()
                        .map(parish -> new CodeNameDTO(parish.getCode(), parish.getName()))
                        .sorted(Comparator.comparing(CodeNameDTO::getCode)).toList();
            }
            case COUNTY -> {
                if (!notNullEmpty(partOfCode)) throw new MissingDataException("Missing Administrative Area partOf");
                List<String> subCountyCodes = subCountyRepository.findAllByCounty_Code(partOfCode).parallelStream()
                        .map(SubCounty::getCode).distinct().toList();
                yield parishRepository.findAllBySubCountyCodes(subCountyCodes).parallelStream()
                        .map(parish -> new CodeNameDTO(parish.getCode(), parish.getName()))
                        .sorted(Comparator.comparing(CodeNameDTO::getCode)).toList();
            }
            case SUBCOUNTY -> {
                if (!notNullEmpty(partOfCode)) throw new MissingDataException("Missing Administrative Area partOf");
                yield parishRepository.findAllBySubCounty_Code(partOfCode).parallelStream()
                        .map(parish -> new CodeNameDTO(parish.getCode(), parish.getName()))
                        .sorted(Comparator.comparing(CodeNameDTO::getCode)).toList();
            }
            case PARISH -> Collections.emptyList();
        };
        return new ResponseDTO<>(codeNameDtoList);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS,
            keyGenerator = "searchKeyGenerator", cacheManager = "hourCacheManager")
    public ResponseDTO<?> searchList(Map<String, String> queryMap) {
        String type = queryMap.get("type");
        String partOf = queryMap.get("partOf");

        AdministrativeAreaType administrativeAreaType = AdministrativeAreaType.fromStr(type)
                .orElseThrow(() -> new MissingDataException("Unsupported Administrative Area Type: " + type));

        return switch (administrativeAreaType) {
            case REGION -> {
                List<RegionDTO> regionDtos = regionRepository.findAll().parallelStream()
                        .map(AdministrativeAreaServiceImpl::convertRegionDTO)
                        .sorted(Comparator.comparing(RegionDTO::getCode)).toList();
                yield new ResponseDTO<>(regionDtos);
            }
            case SUBREGION -> {
                List<SubRegionDTO> subRegionDTOs = (notNullEmpty(partOf)
                        ? subRegionRepository.findAllByRegion_Code(partOf)
                        : subRegionRepository.findAll()).parallelStream()
                        .map(AdministrativeAreaServiceImpl::convertSubRegionDTO)
                        .sorted(Comparator.comparing(SubRegionDTO::getCode)).toList();
                yield new ResponseDTO<>(subRegionDTOs);
            }
            case LOCALGOVERNMENT -> {
                List<LocalGovernmentDTO> localGovernmentDtos = (notNullEmpty(partOf)
                        ? localGovernmentRepository.findAllBySubRegion_Code(partOf)
                        : localGovernmentRepository.findAll()).parallelStream()
                        .map(this::convertLocalGovernmentDTO)
                        .sorted(Comparator.comparing(LocalGovernmentDTO::getCode)).toList();
                yield new ResponseDTO<>(localGovernmentDtos);
            }
            case COUNTY -> {
                List<CountyDTO> countyDtos = (notNullEmpty(partOf)
                        ? countyRepository.findAllByLocalGovernment_Code(partOf)
                        : countyRepository.findAll()).parallelStream()
                        .map(this::convertCountyDTO)
                        .sorted(Comparator.comparing(CountyDTO::getCode)).toList();
                yield new ResponseDTO<>(countyDtos);
            }
            case SUBCOUNTY -> {
                List<SubCountyDTO> subCountyDTOs = (notNullEmpty(partOf)
                        ? subCountyRepository.findAllByCounty_Code(partOf)
                        : subCountyRepository.findAll()).parallelStream()
                        .map(this::convertSubCountyDTO)
                        .sorted(Comparator.comparing(SubCountyDTO::getCode)).toList();
                yield new ResponseDTO<>(subCountyDTOs);
            }
            case PARISH -> {
                List<ParishDTO> parishDtos = (notNullEmpty(partOf)
                        ? parishRepository.findAllBySubCounty_Code(partOf)
                        : parishRepository.findAll()).parallelStream()
                        .map(this::convertParishDTO)
                        .sorted(Comparator.comparing(ParishDTO::getCode)).toList();
                yield new ResponseDTO<>(parishDtos);
            }
        };
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS,
            keyGenerator = "searchKeyGenerator", cacheManager = "hourCacheManager")
    public ResponseDTO<?> searchOne(Map<String, String> queryMap) {
        String type = queryMap.get("type");
        String code = queryMap.get("code");

        if (code == null) {
            throw new MissingDataException("Missing required data");
        }
        AdministrativeAreaType administrativeAreaType = AdministrativeAreaType.fromStr(type)
                .orElseThrow(() -> new MissingDataException("Missing Administrative Area Type"));

        return switch (administrativeAreaType) {
            case REGION -> {
                Region region = regionRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("Region not found"));
                yield new ResponseDTO<>(convertRegionDTO(region));
            }
            case SUBREGION -> {
                SubRegion subRegion = subRegionRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("SubRegion not found"));
                yield new ResponseDTO<>(convertSubRegionDTO(subRegion));
            }
            case LOCALGOVERNMENT -> {
                LocalGovernment localGovernment = localGovernmentRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("LocalGovernment not found"));
                yield new ResponseDTO<>(convertLocalGovernmentDTO(localGovernment));
            }
            case COUNTY -> {
                County county = countyRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("County not found"));
                yield new ResponseDTO<>(convertCountyDTO(county));
            }
            case SUBCOUNTY -> {
                SubCounty subCounty = subCountyRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("SubCounty not found"));
                yield new ResponseDTO<>(convertSubCountyDTO(subCounty));
            }
            case PARISH -> {
                Parish parish = parishRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("Parish not found"));
                yield new ResponseDTO<>(convertParishDTO(parish));
            }
        };
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS_SEARCH,
            keyGenerator = "searchKeyGenerator", cacheManager = "searchCacheManager")
    public PaginatedResponseDTO<AdministrativeAreaDTO> search(Map<String, String> queryMap) {
        AdministrativeAreaType type = AdministrativeAreaType.fromStr(queryMap.get("type"))
                .orElseThrow(() -> new MissingDataException("Missing or unsupported Administrative Area Type"));

        int clientPage = parseInt(queryMap.get("page"), 1);
        int size = parseInt(queryMap.get("size"), 100);
        int dbPage = clientPage <= 1 ? 0 : clientPage - 1;
        String sortBy = queryMap.getOrDefault("sortBy", "code");
        String sortDirection = queryMap.getOrDefault("sortDirection", "asc");
        Pageable pageable = PageRequest.of(dbPage, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));

        String search = queryMap.get("search");
        String partOf = queryMap.get("partOf");

        return switch (type) {
            case REGION -> runSearch(regionRepository, regionSpec(search, partOf, queryMap), pageable, clientPage, size,
                    r -> flatDto(r.getCode(), r.getName(), r.getLatitude(), r.getLongitude()));
            case SUBREGION -> runSearch(subRegionRepository, subRegionSpec(search, partOf, queryMap), pageable, clientPage, size,
                    r -> flatDto(r.getCode(), r.getName(), r.getLatitude(), r.getLongitude()));
            case LOCALGOVERNMENT -> runSearch(localGovernmentRepository, lgSpec(search, partOf, queryMap), pageable, clientPage, size,
                    r -> flatDto(r.getCode(), r.getName(), r.getLatitude(), r.getLongitude()));
            case COUNTY -> runSearch(countyRepository, countySpec(search, partOf, queryMap), pageable, clientPage, size,
                    r -> flatDto(r.getCode(), r.getName(), r.getLatitude(), r.getLongitude()));
            case SUBCOUNTY -> runSearch(subCountyRepository, subCountySpec(search, partOf, queryMap), pageable, clientPage, size,
                    r -> flatDto(r.getCode(), r.getName(), r.getLatitude(), r.getLongitude()));
            case PARISH -> runSearch(parishRepository, parishSpec(search, partOf, queryMap), pageable, clientPage, size,
                    r -> flatDto(r.getCode(), r.getName(), r.getLatitude(), r.getLongitude()));
        };
    }

    private Specification<Region> regionSpec(String search, String partOf, Map<String, String> q) {
        return SpecificationBuilder.<Region>freeText(search, "name", "code")
                .and(SpecificationBuilder.fromQueryMap(q, HANDLED_SEARCH_KEYS));
    }

    private Specification<SubRegion> subRegionSpec(String search, String partOf, Map<String, String> q) {
        Specification<SubRegion> spec = SpecificationBuilder.<SubRegion>freeText(search, "name", "code")
                .and(SpecificationBuilder.fromQueryMap(q, HANDLED_SEARCH_KEYS));
        return notNullEmpty(partOf) ? spec.and(SpecificationBuilder.where("region.code", partOf, MatchType.EQUALS)) : spec;
    }

    private Specification<LocalGovernment> lgSpec(String search, String partOf, Map<String, String> q) {
        Specification<LocalGovernment> spec = SpecificationBuilder.<LocalGovernment>freeText(search, "name", "code")
                .and(SpecificationBuilder.fromQueryMap(q, HANDLED_SEARCH_KEYS));
        return notNullEmpty(partOf) ? spec.and(SpecificationBuilder.where("subRegion.code", partOf, MatchType.EQUALS)) : spec;
    }

    private Specification<County> countySpec(String search, String partOf, Map<String, String> q) {
        Specification<County> spec = SpecificationBuilder.<County>freeText(search, "name", "code")
                .and(SpecificationBuilder.fromQueryMap(q, HANDLED_SEARCH_KEYS));
        return notNullEmpty(partOf) ? spec.and(SpecificationBuilder.where("localGovernment.code", partOf, MatchType.EQUALS)) : spec;
    }

    private Specification<SubCounty> subCountySpec(String search, String partOf, Map<String, String> q) {
        Specification<SubCounty> spec = SpecificationBuilder.<SubCounty>freeText(search, "name", "code")
                .and(SpecificationBuilder.fromQueryMap(q, HANDLED_SEARCH_KEYS));
        return notNullEmpty(partOf) ? spec.and(SpecificationBuilder.where("county.code", partOf, MatchType.EQUALS)) : spec;
    }

    private Specification<Parish> parishSpec(String search, String partOf, Map<String, String> q) {
        Specification<Parish> spec = SpecificationBuilder.<Parish>freeText(search, "name", "code")
                .and(SpecificationBuilder.fromQueryMap(q, HANDLED_SEARCH_KEYS));
        return notNullEmpty(partOf) ? spec.and(SpecificationBuilder.where("subCounty.code", partOf, MatchType.EQUALS)) : spec;
    }

    private <T> PaginatedResponseDTO<AdministrativeAreaDTO> runSearch(
            JpaSpecificationExecutor<T> repo, Specification<T> spec, Pageable pageable,
            int clientPage, int size, Function<T, AdministrativeAreaDTO> mapper) {
        Page<T> result = repo.findAll(spec, pageable);
        List<AdministrativeAreaDTO> data = result.getContent().stream().map(mapper).toList();
        PaginatedResponseDTO<AdministrativeAreaDTO> response = new PaginatedResponseDTO<>(data);
        response.setMessage("Administrative areas fetched successfully");
        response.setStatus(true);
        response.setPage(clientPage);
        response.setSize(size);
        response.setTotalElements(result.getTotalElements());
        response.setTotalPages(result.getTotalPages());
        response.setHasNext(result.hasNext());
        response.setHasPrevious(result.hasPrevious());
        return response;
    }

    private static AdministrativeAreaDTO flatDto(String code, String name, Double latitude, Double longitude) {
        return new AdministrativeAreaDTO(code, name,
                latitude != null ? String.valueOf(latitude) : "",
                longitude != null ? String.valueOf(longitude) : "");
    }

    private static int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public ResponseDTO<String> upload(List<AdministrativeAreaExcelDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            throw new MissingDataException("Upload list cannot be null or empty");
        }
        uploadAsync(dtoList);
        return new ResponseDTO<>("Upload started for " + dtoList.size() + " administrative area(s). Processing in background.");
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

        if (!newParishSet.isEmpty()) {
            List<Parish> newParishes = newParishSet.stream().map(UP -> {
                Parish parish = new Parish();
                parish.setCode(generateCode(AdministrativeAreaType.PARISH));
                parish.setName(UP.name());
                parish.setSubCounty(UP.subCounty());
                return parish;
            }).toList();
            parishRepository.saveAll(newParishes);
        }
    }

    private void uploadSubCounty(List<AdministrativeAreaExcelDTO> dtoList) {
        List<SubCounty> dbSubCounties = subCountyRepository.findAll();

        Set<USubCounty> newSubCountySet = dtoList.parallelStream().filter(dto -> dbSubCounties.stream().noneMatch(dbSubCounty -> {
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
        if (!newSubCountySet.isEmpty()) {
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
                        })).toList();

        uploadParishes(newDtos);
    }

    private void uploadCounty(List<AdministrativeAreaExcelDTO> dtoList) {
        List<County> dbCounties = countyRepository.findAll();

        Set<UCounty> newCountSet = dtoList.parallelStream().filter(dto -> dbCounties.stream().parallel().noneMatch(dbCounty -> {
            LocalGovernment localGovernment = dbCounty.getLocalGovernment();
            SubRegion subRegion = localGovernment.getSubRegion();
            Region region = subRegion.getRegion();
            return (dbCounty.getName().equalsIgnoreCase(dto.getCounty())
                    && localGovernment.getName().equalsIgnoreCase(dto.getLocalGovernment())
                    && subRegion.getName().equalsIgnoreCase(dto.getSubRegion())
                    && region.getName().equalsIgnoreCase(dto.getRegion()));
        })).map(dto -> new UCounty(dto.getCounty(), dto.getDbLocalGovernment())).collect(Collectors.toSet());

        List<County> dbCounties2;
        if (!newCountSet.isEmpty()) {
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
                        })).toList();

        uploadSubCounty(newDtos);
    }

    private void uploadLocalGovernment(List<AdministrativeAreaExcelDTO> dtoList) {
        List<LocalGovernment> dbLocalGovernments = localGovernmentRepository.findAll();

        Set<ULocalGovernment> newLocalGovernmentSet = dtoList.parallelStream().filter(dto -> dbLocalGovernments.stream().noneMatch(dbLocalGovernment -> {
            SubRegion subRegion = dto.getDbSubRegion();
            Region region = dto.getDbRegion();
            return (dbLocalGovernment.getName().equalsIgnoreCase(dto.getLocalGovernment())
                    && subRegion.getName().equalsIgnoreCase(dto.getSubRegion())
                    && region.getName().equalsIgnoreCase(dto.getRegion()));
        })).map(dto -> new ULocalGovernment(dto.getLocalGovernment(), dto.getDbSubRegion())).collect(Collectors.toSet());

        List<LocalGovernment> dbLocalGovernments2;
        if (!newLocalGovernmentSet.isEmpty()) {
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
                        })).toList();

        uploadCounty(newDtos);
    }

    private void uploadSubRegions(List<AdministrativeAreaExcelDTO> dtoList) {
        List<SubRegion> dbSubRegions = subRegionRepository.findAll();

        Set<USubRegion> newSubRegionSet = dtoList.stream()
                .filter(dto -> dbSubRegions.stream().noneMatch(dbSubRegion -> {
                    Region region = dto.getDbRegion();
                    return (dbSubRegion.getName().equalsIgnoreCase(dto.getSubRegion())
                            && region.getName().equalsIgnoreCase(dto.getRegion()));
                }))
                .map(dto -> new USubRegion(dto.getSubRegion(), dto.getDbRegion()))
                .collect(Collectors.toSet());

        List<SubRegion> dbSubRegions2;
        if (!newSubRegionSet.isEmpty()) {
            List<SubRegion> newSubRegions = newSubRegionSet.stream().map(uSubRegion -> {
                SubRegion subRegion = new SubRegion();
                subRegion.setCode(generateCode(AdministrativeAreaType.SUBREGION));
                subRegion.setName(uSubRegion.name());
                subRegion.setRegion(uSubRegion.region());
                return subRegion;
            }).toList();
            subRegionRepository.saveAll(newSubRegions);
            dbSubRegions2 = subRegionRepository.findAll();
        } else {
            dbSubRegions2 = dbSubRegions;
        }

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

        uploadLocalGovernment(newDtos);
    }

    /**
     * Background hierarchical upload (Regions → … → Parishes) in one transaction.
     * On success, all administrative-area caches are evicted.
     */
    @Async
    @Transactional
    public void uploadAsync(List<AdministrativeAreaExcelDTO> dtoList) {
        try {
            log.info("Starting hierarchical upload of {} administrative area(s)", dtoList.size());
            uploadRegions(dtoList);
            evictAreaCaches();
            log.info("Successfully completed hierarchical upload of {} administrative area(s)", dtoList.size());
        } catch (Exception e) {
            log.error("Failed to upload administrative areas: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void uploadRegions(List<AdministrativeAreaExcelDTO> dtoList) {
        List<Region> dbRegions = regionRepository.findAll();

        List<Region> newRegions = dtoList.parallelStream()
                .filter(dto -> dbRegions.stream().noneMatch(dbRegion ->
                        dbRegion.getName().equalsIgnoreCase(dto.getRegion())))
                .filter(distinctByKey(AdministrativeAreaExcelDTO::getRegion))
                .map(dto -> {
                    Region region = new Region();
                    region.setCode(generateCode(AdministrativeAreaType.REGION));
                    region.setName(dto.getRegion());
                    return region;
                })
                .toList();

        List<Region> dbRegions2;
        if (!newRegions.isEmpty()) {
            regionRepository.saveAll(newRegions);
            dbRegions2 = regionRepository.findAll();
        } else {
            dbRegions2 = dbRegions;
        }

        List<AdministrativeAreaExcelDTO> newDtos = dtoList.parallelStream()
                .flatMap(oldDto -> dbRegions2.stream()
                        .filter(region -> region.getName().equalsIgnoreCase(oldDto.getRegion()))
                        .map(region -> {
                            oldDto.setDbRegion(region);
                            return oldDto;
                        }))
                .toList();

        uploadSubRegions(newDtos);
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    private void evictAreaCaches() {
        clearCache(CacheValueKeyConfig.ADMINISTRATIVE_AREAS);
        clearCache(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER);
        clearCache(CacheValueKeyConfig.ADMINISTRATIVE_AREAS_SEARCH);
    }

    private void clearCache(String name) {
        var cache = cacheManager.getCache(name);
        if (cache != null) {
            cache.clear();
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS, allEntries = true),
            @CacheEvict(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, allEntries = true),
            @CacheEvict(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS_SEARCH, allEntries = true)
    })
    public ResponseDTO<String> updateOne(Map<String, String> queryMap, UpdateAdministrativeAreaDTO dto) {
        AdministrativeAreaType administrativeAreaType = AdministrativeAreaType.fromStr(queryMap.get("type"))
                .orElseThrow(() -> new MissingDataException("Missing Administrative Area Type"));

        return switch (administrativeAreaType) {
            case REGION -> {
                Region region = regionRepository.findByCodeIgnoreCase(dto.getCode()).orElseThrow(() -> new InvalidException("Invalid PartOfCode"));
                if (notNullEmpty(dto.getName())) region.setName(dto.getName());
                if (notNullEmpty(dto.getLatitude())) region.setLatitude(Double.valueOf(dto.getLatitude()));
                if (notNullEmpty(dto.getLongitude())) region.setLongitude(Double.valueOf(dto.getLongitude()));
                if (notNullEmpty(dto.getDescription())) region.setDescription(dto.getDescription());
                regionRepository.save(region);
                yield new ResponseDTO<>("SUCCESS");
            }
            case SUBREGION -> {
                if (nullEmpty(dto.getPartOfCode())) throw new MissingDataException("Missing PartOfCode");
                Region region = regionRepository.findByCodeIgnoreCase(dto.getPartOfCode()).orElseThrow(() -> new InvalidException("Invalid PartOfCode"));
                SubRegion subRegion = subRegionRepository.findByCodeIgnoreCase(dto.getCode()).orElseThrow(() -> new NotFoundException("Administrative Area NotFound"));
                if (notNullEmpty(dto.getName())) subRegion.setName(dto.getName());
                if (notNullEmpty(dto.getLatitude())) subRegion.setLatitude(Double.valueOf(dto.getLatitude()));
                if (notNullEmpty(dto.getLongitude())) subRegion.setLongitude(Double.valueOf(dto.getLongitude()));
                if (notNullEmpty(dto.getDescription())) subRegion.setDescription(dto.getDescription());
                subRegion.setRegion(region);
                subRegionRepository.save(subRegion);
                yield new ResponseDTO<>("SUCCESS");
            }
            case LOCALGOVERNMENT -> {
                if (nullEmpty(dto.getPartOfCode())) throw new MissingDataException("Missing PartOfCode");
                SubRegion subRegion = subRegionRepository.findByCodeIgnoreCase(dto.getPartOfCode()).orElseThrow(() -> new InvalidException("Invalid PartOfCode"));
                LocalGovernment localGovernment = localGovernmentRepository.findByCodeIgnoreCase(dto.getCode()).orElseThrow(() -> new NotFoundException("Administrative Area NotFound"));
                if (notNullEmpty(dto.getName())) localGovernment.setName(dto.getName());
                if (notNullEmpty(dto.getLatitude())) localGovernment.setLatitude(Double.valueOf(dto.getLatitude()));
                if (notNullEmpty(dto.getLongitude())) localGovernment.setLongitude(Double.valueOf(dto.getLongitude()));
                if (notNullEmpty(dto.getDescription())) localGovernment.setDescription(dto.getDescription());
                localGovernment.setSubRegion(subRegion);
                localGovernmentRepository.save(localGovernment);
                yield new ResponseDTO<>("SUCCESS");
            }
            case COUNTY -> {
                if (nullEmpty(dto.getPartOfCode())) throw new MissingDataException("Missing PartOfCode");
                LocalGovernment localGovernment = localGovernmentRepository.findByCodeIgnoreCase(dto.getPartOfCode()).orElseThrow(() -> new InvalidException("Invalid PartOfCode"));
                County county = countyRepository.findByCodeIgnoreCase(dto.getCode()).orElseThrow(() -> new NotFoundException("Administrative Area NotFound"));
                if (notNullEmpty(dto.getName())) county.setName(dto.getName());
                if (notNullEmpty(dto.getLatitude())) county.setLatitude(Double.valueOf(dto.getLatitude()));
                if (notNullEmpty(dto.getLongitude())) county.setLongitude(Double.valueOf(dto.getLongitude()));
                if (notNullEmpty(dto.getDescription())) county.setDescription(dto.getDescription());
                county.setLocalGovernment(localGovernment);
                countyRepository.save(county);
                yield new ResponseDTO<>("SUCCESS");
            }
            case SUBCOUNTY -> {
                if (nullEmpty(dto.getPartOfCode())) throw new MissingDataException("Missing PartOfCode");
                County county = countyRepository.findByCodeIgnoreCase(dto.getPartOfCode()).orElseThrow(() -> new InvalidException("Invalid PartOfCode"));
                SubCounty subCounty = subCountyRepository.findByCodeIgnoreCase(dto.getCode()).orElseThrow(() -> new NotFoundException("Administrative Area NotFound"));
                if (notNullEmpty(dto.getName())) subCounty.setName(dto.getName());
                if (notNullEmpty(dto.getLatitude())) subCounty.setLatitude(Double.valueOf(dto.getLatitude()));
                if (notNullEmpty(dto.getLongitude())) subCounty.setLongitude(Double.valueOf(dto.getLongitude()));
                if (notNullEmpty(dto.getDescription())) subCounty.setDescription(dto.getDescription());
                subCounty.setCounty(county);
                subCountyRepository.save(subCounty);
                yield new ResponseDTO<>("SUCCESS");
            }
            case PARISH -> {
                if (nullEmpty(dto.getPartOfCode())) throw new MissingDataException("Missing PartOfCode");
                SubCounty subCounty = subCountyRepository.findByCodeIgnoreCase(dto.getPartOfCode()).orElseThrow(() -> new InvalidException("Invalid PartOfCode"));
                Parish parish = parishRepository.findByCodeIgnoreCase(dto.getCode()).orElseThrow(() -> new NotFoundException("Administrative Area NotFound"));
                if (notNullEmpty(dto.getName())) parish.setName(dto.getName());
                if (notNullEmpty(dto.getLatitude())) parish.setLatitude(Double.valueOf(dto.getLatitude()));
                if (notNullEmpty(dto.getLongitude())) parish.setLongitude(Double.valueOf(dto.getLongitude()));
                if (notNullEmpty(dto.getDescription())) parish.setDescription(dto.getDescription());
                parish.setSubCounty(subCounty);
                parishRepository.save(parish);
                yield new ResponseDTO<>("SUCCESS");
            }
        };
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS, allEntries = true),
            @CacheEvict(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS_FILTER, allEntries = true),
            @CacheEvict(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS_SEARCH, allEntries = true)
    })
    public ResponseDTO<String> delete(Map<String, String> queryMap) {
        String code = queryMap.get("code");
        if (!notNullEmpty(code)) {
            throw new MissingDataException("Missing Administrative Area Code");
        }
        AdministrativeAreaType administrativeAreaType = AdministrativeAreaType.fromStr(queryMap.get("type"))
                .orElseThrow(() -> new MissingDataException("Missing Administrative Area Type"));

        switch (administrativeAreaType) {
            case REGION -> {
                Region region = regionRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("Region not found"));
                if (!subRegionRepository.findAllByRegion_Code(code).isEmpty()) {
                    throw new InvalidException("Cannot delete a region that still has sub-regions");
                }
                regionRepository.delete(region);
            }
            case SUBREGION -> {
                SubRegion subRegion = subRegionRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("SubRegion not found"));
                if (!localGovernmentRepository.findAllBySubRegion_Code(code).isEmpty()) {
                    throw new InvalidException("Cannot delete a sub-region that still has local governments");
                }
                subRegionRepository.delete(subRegion);
            }
            case LOCALGOVERNMENT -> {
                LocalGovernment localGovernment = localGovernmentRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("LocalGovernment not found"));
                if (!countyRepository.findAllByLocalGovernment_Code(code).isEmpty()) {
                    throw new InvalidException("Cannot delete a local government that still has counties");
                }
                localGovernmentRepository.delete(localGovernment);
            }
            case COUNTY -> {
                County county = countyRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("County not found"));
                if (!subCountyRepository.findAllByCounty_Code(code).isEmpty()) {
                    throw new InvalidException("Cannot delete a county that still has sub-counties");
                }
                countyRepository.delete(county);
            }
            case SUBCOUNTY -> {
                SubCounty subCounty = subCountyRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("SubCounty not found"));
                if (!parishRepository.findAllBySubCounty_Code(code).isEmpty()) {
                    throw new InvalidException("Cannot delete a sub-county that still has parishes");
                }
                subCountyRepository.delete(subCounty);
            }
            case PARISH -> {
                Parish parish = parishRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("Parish not found"));
                parishRepository.delete(parish);
            }
        }
        return new ResponseDTO<>("SUCCESS", "Administrative area deleted successfully");
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
