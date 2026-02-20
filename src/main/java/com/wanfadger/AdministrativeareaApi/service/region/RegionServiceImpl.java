package com.wanfadger.AdministrativeareaApi.service.region;

import com.wanfadger.AdministrativeareaApi.areaexceptions.AlreadyExistsException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.MissingDataException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.NotFoundException;

import com.wanfadger.AdministrativeareaApi.dto.ExcelJsonDTO;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.RegionDTO;
import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.entity.Region;
import com.wanfadger.AdministrativeareaApi.repository.RegionRepository;

import com.wanfadger.AdministrativeareaApi.repository.SubRegionRepository;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.wanfadger.AdministrativeareaApi.repository.specification.GenericSpecification;
import com.wanfadger.AdministrativeareaApi.shared.SharedService;

import com.wanfadger.AdministrativeareaApi.enums.MatchType;
import com.wanfadger.AdministrativeareaApi.dto.SearchCriteria;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.wanfadger.AdministrativeareaApi.config.CacheValueKeyConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegionServiceImpl implements RegionService {

    private final RegionRepository regionRepository;
    private final RegionMapperService regionMapperService;
    private final SharedService sharedService;

    @Override
    @Transactional
    @CacheEvict(value = CacheValueKeyConfig.REGIONS, allEntries = true)
    public ResponseDTO<String> create(NewAdministrativeAreaDTO dto) {
        if (regionRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new AlreadyExistsException(
                    AdministrativeAreaType.REGION.getAreaType() + " with name " + dto.getName() + " Already Exists");
        }

        Region region = regionMapperService.toRegion(dto);
        regionRepository.save(region);
        return new ResponseDTO<>(region.getCode(), "successfully created a region");
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheValueKeyConfig.REGIONS, allEntries = true)
    public ResponseDTO<String> createList(List<NewAdministrativeAreaDTO> dtos) {
        List<Region> regions = dtos.parallelStream()
                .filter(dto -> !regionRepository.existsByNameIgnoreCase(dto.getName()))
                .map(regionMapperService::toRegion)
                .toList();

        regionRepository.saveAll(Objects.requireNonNull(regions));

        return new ResponseDTO<>("success",
                "successfully added " + regions.size() + " administrative areas");
    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.REGIONS, CacheValueKeyConfig.REGIONS_FILTERED }, allEntries = true)
    public ResponseDTO<String> update(UpdateAdministrativeAreaDTO dto) {
        if (dto.getCode() == null || dto.getCode().isEmpty()) {
            throw new MissingDataException("Missing Code");
        }

        Region region = regionRepository.findByCodeIgnoreCase(dto.getCode())
                .orElseThrow(() -> new NotFoundException("Region with code " + dto.getCode() + " not found"));

        if (dto.getName() != null && !dto.getName().isEmpty() && !region.getName().equalsIgnoreCase(dto.getName())) {
            if (regionRepository.existsByNameIgnoreCase(dto.getName())) {
                throw new AlreadyExistsException(AdministrativeAreaType.REGION.getAreaType() + " with name "
                        + dto.getName() + " Already Exists");
            }
            region.setName(dto.getName());
        }

        if (dto.getLatitude() != null && !dto.getLatitude().isEmpty()
                && !region.getLatitude().toString().equalsIgnoreCase(dto.getLatitude())) {
            region.setLatitude(Double.valueOf(dto.getLatitude()));
        }

        if (dto.getLongitude() != null && !dto.getLongitude().isEmpty()
                && !region.getLongitude().toString().equalsIgnoreCase(dto.getLongitude())) {
            region.setLongitude(Double.valueOf(dto.getLongitude()));
        }

        if (dto.getDescription() != null && !dto.getDescription().isEmpty()
                && !region.getDescription().equalsIgnoreCase(dto.getDescription())) {
            region.setDescription(dto.getDescription());
        }

        regionRepository.save(region);

        return new ResponseDTO<>(region.getCode(), "successfully updated region");
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.REGIONS, key = "#code")
    public ResponseDTO<RegionDTO> findByCode(String code) {
        Region region = regionRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("Region with code " + code + " not found"));
        return new ResponseDTO<>(regionMapperService.toDTO(region));
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.REGIONS, key = "#code")
    public ResponseDTO<RegionDTO> findDetailsByCode(String code) {
        Region region = regionRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("Region with code " + code + " not found"));
        return new ResponseDTO<>(regionMapperService.toDTO(region));
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.REGIONS, key = "#queryMap.toString()", unless = "#result.totalElements == 0")
    public PaginatedResponseDTO<RegionDTO> search(Map<String, String> queryMap) {
        // 1. Extract Pagination & Sorting
        int page = Optional.ofNullable(queryMap.get("page")).map(Integer::parseInt).orElse(1);
        int size = Optional.ofNullable(queryMap.get("size")).map(Integer::parseInt).orElse(10);
        String sortBy = Optional.ofNullable(queryMap.get("sortBy")).orElse("name");
        String sortDirection = Optional.ofNullable(queryMap.get("sortDirection")).orElse("ASC");

        page = page <= 0 ? 0 : page - 1;

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(sortDirection.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy));

        // 2. Build Specification
        Specification<Region> spec = (root, query, cb) -> cb.conjunction();
        spec = spec.and(new GenericSpecification<>(new SearchCriteria("archived", "false", MatchType.EQUALS)));
        for (Map.Entry<String, String> entry : queryMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (List.of("page", "size", "sortBy", "sortDirection", "type").contains(key))
                continue;

            MatchType matchType = MatchType.EQUALS;
            if (key.contains(":")) {
                String[] parts = key.split(":", 2);
                key = parts[0];
                try {
                    matchType = MatchType.valueOf(parts[1].toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid MatchType: {}, defaulting to EQUALS", parts[1]);
                }
            }
            spec = spec.and(new GenericSpecification<>(new SearchCriteria(key, value, matchType)));
        }

        // 3. Execute Search
        Page<Region> resultPage = regionRepository.findAll(spec, pageable);

        // 4. Map to DTOs
        List<RegionDTO> data = resultPage.getContent().stream()
                .map(regionMapperService::toDTO)
                .toList();

        // 5. Build Paginated Response
        PaginatedResponseDTO<RegionDTO> response = new PaginatedResponseDTO<>();
        response.setData(data);
        response.setPage(resultPage.getNumber() + 1);
        response.setSize(resultPage.getSize());
        response.setTotalElements(resultPage.getTotalElements());
        response.setTotalPages(resultPage.getTotalPages());
        response.setHasNext(resultPage.hasNext());
        response.setHasPrevious(resultPage.hasPrevious());
        response.setMessage("success");
        response.setStatus(true);

        return response;
    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.REGIONS, CacheValueKeyConfig.REGIONS_FILTERED }, allEntries = true)
    public List<Region> upload(List<ExcelJsonDTO> excelJsonDtos) {
        List<String> regionNames = excelJsonDtos.stream()
                .map(ExcelJsonDTO::getRegion)
                .filter(r -> r != null && !r.isEmpty())
                .map(String::trim)
                .distinct()
                .filter(r -> !regionRepository.existsByNameIgnoreCase(r))
                .toList();

        List<Region> regions = regionNames.stream()
                .map(name -> {
                    Region region = Region.builder()
                            .name(name)
                            .code(sharedService.generateUniqueCode(AdministrativeAreaType.REGION,
                                    code -> regionRepository.existsByCodeIgnoreCase(code)))
                            .areaType(AdministrativeAreaType.REGION)
                            .description(name)
                            .build();
                    return region;
                })
                .toList();

        if (regions.size() > 0) {
            regionRepository.saveAll(regions);
        }

        int page = 0;
        int size = 100;
        List<Region> existingRegions = new ArrayList<>();
        Specification<Region> spec = (root, query, cb) -> cb.conjunction();
        spec = spec.and(new GenericSpecification<>(new SearchCriteria("archived", "false", MatchType.EQUALS)));
        Page<Region> regionPage = regionRepository.findAll(spec, PageRequest.of(page, size));
        while (regionPage.hasNext()) {
            existingRegions.addAll(regionPage.getContent());
            regionPage = regionRepository.findAll(spec, PageRequest.of(++page, size));
        }
        return existingRegions;
    }

    @Override
    @Transactional
    public List<Region> saveAll(List<Region> regions) {
        return regionRepository.saveAll(regions);
    }

    private final SubRegionRepository subRegionRepository;

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.REGIONS, CacheValueKeyConfig.REGIONS_FILTERED }, allEntries = true)
    public ResponseDTO<String> delete(String code) {
        if (subRegionRepository.existsByRegion_Code(code)) {
            throw new InvalidException("Region with code " + code + " cannot be deleted because it has sub regions");
        }

        Region region = regionRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("Region with code " + code + " not found"));
        regionRepository.delete(region);
        return new ResponseDTO<>("SUCCESS", "Region deleted successfully");
    }

}
