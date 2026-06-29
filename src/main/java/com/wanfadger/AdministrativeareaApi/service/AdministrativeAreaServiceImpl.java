package com.wanfadger.AdministrativeareaApi.service;

import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.AlreadyExistsException;
import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.MissingDataException;
import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.NotFoundException;
import com.wanfadger.AdministrativeareaApi.beanConfig.CacheValueKeyConfig;
import com.wanfadger.AdministrativeareaApi.dto.*;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.entity.*;
import com.wanfadger.AdministrativeareaApi.repository.*;
import com.wanfadger.AdministrativeareaApi.repository.specification.MatchType;
import com.wanfadger.AdministrativeareaApi.repository.specification.SpecificationBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;

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

    /** Query keys consumed directly by the search method, excluded from generic column filters. */
    private static final Set<String> HANDLED_SEARCH_KEYS = Set.of("type", "partOf");

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
            @CacheEvict(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS_SEARCH, allEntries = true)
    })
    public ResponseDTO<List<String>> create(Map<String, String> queryMap, List<NewAdministrativeAreaDTO> dtos) {
        AdministrativeAreaType type = AdministrativeAreaType.fromStr(queryMap.get("type"))
                .orElseThrow(() -> new MissingDataException("Missing Administrative Area Type"));
        if (dtos == null || dtos.isEmpty()) {
            throw new MissingDataException("Request body must contain at least one administrative area");
        }
        List<String> codes = new ArrayList<>(dtos.size());
        for (NewAdministrativeAreaDTO dto : dtos) {
            codes.add(createOne(type, dto));
        }
        return new ResponseDTO<>(codes, "successfully created " + codes.size() + " administrative area(s)");
    }

    /** Validate parent + uniqueness, persist a single area of the given type, and return its code. */
    private String createOne(AdministrativeAreaType administrativeAreaType, NewAdministrativeAreaDTO dto) {
        return switch (administrativeAreaType) {
            case REGION -> {
                if (regionRepository.findByNameIgnoreCase(dto.getName()).isPresent()) {
                    throw new AlreadyExistsException("Administrative Area Already Exists");
                }
                Region region = convertDtoRegion(dto, administrativeAreaType);
                regionRepository.save(region);
                yield region.getCode();
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
                yield subRegion.getCode();
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
                yield localGovernment.getCode();
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
                yield county.getCode();
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
                yield subCounty.getCode();
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
                yield parish.getCode();
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
    @Transactional(readOnly = true)
    @Cacheable(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS,
            keyGenerator = "searchKeyGenerator")
    public ResponseDTO<AdministrativeAreaDTO> getOne(Map<String, String> queryMap) {
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
            keyGenerator = "searchKeyGenerator")
    public PaginatedResponseDTO<AdministrativeAreaDTO> search(Map<String, String> queryMap) {
        AdministrativeAreaType type = AdministrativeAreaType.fromStr(queryMap.get("type"))
                .orElseThrow(() -> new MissingDataException("Missing or unsupported Administrative Area Type"));

        int clientPage = parseInt(queryMap.get("page"), 1);
        // Default page size is large (1000): parishes are numerous, while the higher levels
        // never exceed it and so always collapse to a single (cached) page.
        int size = parseInt(queryMap.get("size"), 1000);
        int dbPage = clientPage <= 1 ? 0 : clientPage - 1;
        // Default ordering is alphabetical by name.
        String sortBy = queryMap.getOrDefault("sortBy", "name");
        String sortDirection = queryMap.getOrDefault("sortDirection", "asc");
        Pageable pageable = PageRequest.of(dbPage, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));

        String search = queryMap.get("search");
        String partOf = queryMap.get("partOf");

        return switch (type) {
            case REGION -> runSearch(regionRepository, regionSpec(search, partOf, queryMap), pageable, clientPage, size,
                    AdministrativeAreaServiceImpl::convertRegionDTO);
            case SUBREGION -> runSearch(subRegionRepository, subRegionSpec(search, partOf, queryMap), pageable, clientPage, size,
                    AdministrativeAreaServiceImpl::convertSubRegionDTO);
            case LOCALGOVERNMENT -> runSearch(localGovernmentRepository, lgSpec(search, partOf, queryMap), pageable, clientPage, size,
                    this::convertLocalGovernmentDTO);
            case COUNTY -> runSearch(countyRepository, countySpec(search, partOf, queryMap), pageable, clientPage, size,
                    this::convertCountyDTO);
            case SUBCOUNTY -> runSearch(subCountyRepository, subCountySpec(search, partOf, queryMap), pageable, clientPage, size,
                    this::convertSubCountyDTO);
            case PARISH -> runSearch(parishRepository, parishSpec(search, partOf, queryMap), pageable, clientPage, size,
                    this::convertParishDTO);
        };
    }

    private Specification<Region> regionSpec(String search, String partOf, Map<String, String> q) {
        return SpecificationBuilder.<Region>freeText(search, "name", "code")
                .and(SpecificationBuilder.fromQueryMap(q, HANDLED_SEARCH_KEYS));
    }

    private Specification<SubRegion> subRegionSpec(String search, String partOf, Map<String, String> q) {
        Specification<SubRegion> spec = SpecificationBuilder.<SubRegion>freeText(search, "name", "code")
                .and(SpecificationBuilder.fromQueryMap(q, HANDLED_SEARCH_KEYS))
                .and(SpecificationBuilder.fetch("region"));
        return notNullEmpty(partOf) ? spec.and(SpecificationBuilder.where("region.code", partOf, MatchType.EQUALS)) : spec;
    }

    private Specification<LocalGovernment> lgSpec(String search, String partOf, Map<String, String> q) {
        Specification<LocalGovernment> spec = SpecificationBuilder.<LocalGovernment>freeText(search, "name", "code")
                .and(SpecificationBuilder.fromQueryMap(q, HANDLED_SEARCH_KEYS))
                .and(SpecificationBuilder.fetch("subRegion", "region"));
        return notNullEmpty(partOf) ? spec.and(SpecificationBuilder.where("subRegion.code", partOf, MatchType.EQUALS)) : spec;
    }

    private Specification<County> countySpec(String search, String partOf, Map<String, String> q) {
        Specification<County> spec = SpecificationBuilder.<County>freeText(search, "name", "code")
                .and(SpecificationBuilder.fromQueryMap(q, HANDLED_SEARCH_KEYS))
                .and(SpecificationBuilder.fetch("localGovernment", "subRegion", "region"));
        return notNullEmpty(partOf) ? spec.and(SpecificationBuilder.where("localGovernment.code", partOf, MatchType.EQUALS)) : spec;
    }

    private Specification<SubCounty> subCountySpec(String search, String partOf, Map<String, String> q) {
        Specification<SubCounty> spec = SpecificationBuilder.<SubCounty>freeText(search, "name", "code")
                .and(SpecificationBuilder.fromQueryMap(q, HANDLED_SEARCH_KEYS))
                .and(SpecificationBuilder.fetch("county", "localGovernment", "subRegion", "region"));
        return notNullEmpty(partOf) ? spec.and(SpecificationBuilder.where("county.code", partOf, MatchType.EQUALS)) : spec;
    }

    private Specification<Parish> parishSpec(String search, String partOf, Map<String, String> q) {
        Specification<Parish> spec = SpecificationBuilder.<Parish>freeText(search, "name", "code")
                .and(SpecificationBuilder.fromQueryMap(q, HANDLED_SEARCH_KEYS))
                .and(SpecificationBuilder.fetch("subCounty", "county", "localGovernment", "subRegion", "region"));
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

    private static int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS, allEntries = true),
            @CacheEvict(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS_SEARCH, allEntries = true)
    })
    public ResponseDTO<String> updateOne(Map<String, String> queryMap, NewAdministrativeAreaDTO dto) {
        AdministrativeAreaType administrativeAreaType = AdministrativeAreaType.fromStr(queryMap.get("type"))
                .orElseThrow(() -> new MissingDataException("Missing Administrative Area Type"));
        // The code of the area being updated comes from the path (queryMap), not the body.
        String code = queryMap.get("code");
        if (!notNullEmpty(code)) {
            throw new MissingDataException("Missing Administrative Area Code");
        }

        return switch (administrativeAreaType) {
            case REGION -> {
                Region region = regionRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new InvalidException("Invalid PartOfCode"));
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
                SubRegion subRegion = subRegionRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("Administrative Area NotFound"));
                String subRegionName = notNullEmpty(dto.getName()) ? dto.getName() : subRegion.getName();
                subRegionRepository.findByNameIgnoreCaseAndRegion_Code(subRegionName, dto.getPartOfCode())
                        .filter(found -> !found.getCode().equalsIgnoreCase(code))
                        .ifPresent(x -> { throw new AlreadyExistsException("Sub region already exists in the region"); });
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
                LocalGovernment localGovernment = localGovernmentRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("Administrative Area NotFound"));
                String localGovernmentName = notNullEmpty(dto.getName()) ? dto.getName() : localGovernment.getName();
                localGovernmentRepository.findByNameIgnoreCaseAndSubRegion_Code(localGovernmentName, dto.getPartOfCode())
                        .filter(found -> !found.getCode().equalsIgnoreCase(code))
                        .ifPresent(x -> { throw new AlreadyExistsException("Local Government already exists in the sub region"); });
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
                County county = countyRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("Administrative Area NotFound"));
                String countyName = notNullEmpty(dto.getName()) ? dto.getName() : county.getName();
                countyRepository.findByNameIgnoreCaseAndLocalGovernment_Code(countyName, dto.getPartOfCode())
                        .filter(found -> !found.getCode().equalsIgnoreCase(code))
                        .ifPresent(x -> { throw new AlreadyExistsException("County already exists in the local government"); });
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
                SubCounty subCounty = subCountyRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("Administrative Area NotFound"));
                String subCountyName = notNullEmpty(dto.getName()) ? dto.getName() : subCounty.getName();
                subCountyRepository.findByNameIgnoreCaseAndCounty_Code(subCountyName, dto.getPartOfCode())
                        .filter(found -> !found.getCode().equalsIgnoreCase(code))
                        .ifPresent(x -> { throw new AlreadyExistsException("Sub County already exists in the county"); });
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
                Parish parish = parishRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("Administrative Area NotFound"));
                String parishName = notNullEmpty(dto.getName()) ? dto.getName() : parish.getName();
                parishRepository.findByNameIgnoreCaseAndSubCounty_Code(parishName, dto.getPartOfCode())
                        .filter(found -> !found.getCode().equalsIgnoreCase(code))
                        .ifPresent(x -> { throw new AlreadyExistsException("Parish already exists in the sub county"); });
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
