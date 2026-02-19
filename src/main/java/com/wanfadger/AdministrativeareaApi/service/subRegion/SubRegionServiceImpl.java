package com.wanfadger.AdministrativeareaApi.service.subRegion;

import com.wanfadger.AdministrativeareaApi.areaexceptions.AlreadyExistsException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.MissingDataException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.NotFoundException;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaExcelDTO;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;

import com.wanfadger.AdministrativeareaApi.dto.SubRegionDTO;
import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.uniqueDtos.USubRegion;
import com.wanfadger.AdministrativeareaApi.entity.Region;
import com.wanfadger.AdministrativeareaApi.entity.SubRegion;
import com.wanfadger.AdministrativeareaApi.repository.RegionRepository;
import com.wanfadger.AdministrativeareaApi.repository.SubRegionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.wanfadger.AdministrativeareaApi.repository.specification.GenericSpecification;
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

import java.util.List;
import java.util.Map;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubRegionServiceImpl implements SubRegionService {

    private final SubRegionRepository subRegionRepository;
    private final RegionRepository regionRepository;
    private final SubRegionMapperService subRegionMapperService;

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.SUB_REGIONS }, allEntries = true)
    public ResponseDTO<String> create(NewAdministrativeAreaDTO dto) {
        if (dto.getPartOfCode() == null || dto.getPartOfCode().isEmpty()) {
            throw new MissingDataException("Missing PartOfCode(region) for the sub region");
        }

        Region region = regionRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                .orElseThrow(() -> new NotFoundException("Region with code: " + dto.getPartOfCode() + " not found"));

        if (subRegionRepository.existsByNameIgnoreCaseAndRegion_Code(dto.getName(), dto.getPartOfCode())) {
            throw new AlreadyExistsException(
                    dto.getName() + " Sub region Already Exists in the " + region.getName() + " region");
        }

        SubRegion subRegion = subRegionMapperService.toSubRegion(dto, region);

        subRegionRepository.save(subRegion);

        return new ResponseDTO<>(subRegion.getCode(), "successfully created a sub region");
    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.SUB_REGIONS }, allEntries = true)
    public ResponseDTO<String> createAll(List<NewAdministrativeAreaDTO> dtos) {
        // check if all have PartOfCode
        if (dtos.parallelStream().anyMatch(dto -> dto.getPartOfCode() == null || dto.getPartOfCode().isEmpty())) {
            throw new MissingDataException("Found Administrative Area without PartOfCoce");
        }

        List<String> regionCodes = dtos.stream()
                .map(NewAdministrativeAreaDTO::getPartOfCode)
                .collect(Collectors.toList());

        Map<String, Region> regionMap = regionRepository.findByCodeIgnoreCaseIn(regionCodes).stream()
                .collect(Collectors.toMap(Region::getCode, region -> region, (existing, replacement) -> existing));

        List<SubRegion> subRegions = dtos.parallelStream()
                .filter(dto -> subRegionRepository
                        .existsByNameIgnoreCaseAndRegion_Code(dto.getName(), dto.getPartOfCode()))
                .filter(dto -> regionMap.containsKey(dto.getPartOfCode()))
                .map(dto -> {
                    Region region = regionMap.get(dto.getPartOfCode());
                    return subRegionMapperService.toSubRegion(dto, region);
                })
                .toList();

        subRegionRepository.saveAll(subRegions);

        return new ResponseDTO<>("success",
                "successfully added " + subRegions.size() + " sub regions");
    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.SUB_REGIONS }, allEntries = true)
    public ResponseDTO<String> update(String code, UpdateAdministrativeAreaDTO dto) {
        if (dto.getPartOfCode() == null || dto.getPartOfCode().isEmpty()) {
            throw new MissingDataException("Missing PartOfCode");
        }

        Region region = regionRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                .orElseThrow(() -> new InvalidException("Invalid PartOfCode"));
        SubRegion subRegion = subRegionRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("Sub Region NotFound"));

        if (dto.getName() != null && !dto.getName().isEmpty()) {
            subRegion.setName(dto.getName());
        }

        if (dto.getLatitude() != null && !dto.getLatitude().isEmpty()) {
            subRegion.setLatitude(Double.valueOf(dto.getLatitude()));
        }

        if (dto.getLongitude() != null && !dto.getLongitude().isEmpty()) {
            subRegion.setLongitude(Double.valueOf(dto.getLongitude()));
        }

        if (dto.getDescription() != null && !dto.getDescription().isEmpty()) {
            subRegion.setDescription(dto.getDescription());
        }

        subRegion.setRegion(region);

        subRegionRepository.save(subRegion);

        return new ResponseDTO<>("SUCCESS");
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.SUB_REGIONS, key = "#code")
    @Transactional(readOnly = true)
    public ResponseDTO<SubRegionDTO> findByCode(String code) {
        SubRegion subRegion = subRegionRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("Sub Region with code " + code + " not found"));
        return new ResponseDTO<>(subRegionMapperService.toDTO(subRegion));
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.SUB_REGIONS, key = "#code+'_details'")
    @Transactional(readOnly = true)
    public ResponseDTO<SubRegionDTO> findDetailsByCode(String code) {
        SubRegion subRegion = subRegionRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("Sub Region with code " + code + " not found"));
        return new ResponseDTO<>(subRegionMapperService.toDTO(subRegion));
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.SUB_REGIONS, keyGenerator = "sortedMapKeyGenerator", unless = "#result.totalElements == 0")
    @Transactional(readOnly = true)
    public PaginatedResponseDTO<SubRegionDTO> search(Map<String, String> queryMap) {
        // 1. Extract Pagination & Sorting
        int page = Optional.ofNullable(queryMap.get("page")).map(Integer::parseInt).orElse(1);
        int size = Optional.ofNullable(queryMap.get("size")).map(Integer::parseInt).orElse(10);
        String sortBy = Optional.ofNullable(queryMap.get("sortBy")).orElse("name");
        String sortDirection = Optional.ofNullable(queryMap.get("sortDirection")).orElse("ASC");

        page = page <= 0 ? 0 : page - 1;

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(sortDirection.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy));

        // 2. Build Specification
        Specification<SubRegion> spec = (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType()) {
                root.fetch("region", jakarta.persistence.criteria.JoinType.LEFT); // 3. Fetch Region to avoid n+1
            }
            return cb.conjunction();
        };
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

        // 4. Execute Search
        Page<SubRegion> resultPage = subRegionRepository.findAll(spec, pageable);

        // 4. Map to DTOs
        List<SubRegionDTO> data = resultPage.getContent().stream()
                .map(subRegionMapperService::toDetailDTO)
                .toList();

        // 5. Build Paginated Response
        PaginatedResponseDTO<SubRegionDTO> response = new PaginatedResponseDTO<>();
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
    @CacheEvict(value = { CacheValueKeyConfig.SUB_REGIONS }, allEntries = true)
    public void upload(List<AdministrativeAreaExcelDTO> dtoList) {
        // Step 1: Get all existing sub-regions from database
        List<SubRegion> dbSubRegions = subRegionRepository.findAll();

        // Step 2: Extract unique new sub-regions (exclude existing ones)
        Set<USubRegion> newSubRegionSet = dtoList.stream()
                .filter(dto -> dbSubRegions.stream().noneMatch(dbSubRegion -> {
                    Region region = dto.getDbRegion();
                    if (region == null && dbSubRegion.getRegion() == null)
                        return dbSubRegion.getName().equalsIgnoreCase(dto.getSubRegion());
                    if (region == null || dbSubRegion.getRegion() == null)
                        return false;

                    return (dbSubRegion.getName().equalsIgnoreCase(dto.getSubRegion())
                            && region.getName().equalsIgnoreCase(dto.getRegion()));
                }))
                .map(dto -> new USubRegion(dto.getSubRegion(), dto.getDbRegion().getId()))
                .collect(Collectors.toSet());

        // Step 3: Save new sub-regions and fetch updated complete list
        List<SubRegion> dbSubRegions2;
        if (newSubRegionSet.size() > 0) {
            List<SubRegion> newSubRegions = newSubRegionSet.stream().map(uSubRegion -> {
                SubRegion subRegion = new SubRegion();
                // subRegion.setCode(generateCode());
                subRegion.setName(uSubRegion.getName());
                subRegion.setRegion(regionRepository.findById(uSubRegion.getId())
                        .orElseThrow(() -> new NotFoundException("Region not found")));
                return subRegion;
            }).toList();
            subRegionRepository.saveAll(newSubRegions);
            dbSubRegions2 = subRegionRepository.findAll(); // Fetch updated list
        } else {
            dbSubRegions2 = dbSubRegions; // No new sub-regions, use existing list
        }

        // Step 4: Update DTOs with DB sub-region references
        dtoList.parallelStream().forEach(oldDto -> {
            dbSubRegions2.stream()
                    .filter(dbSubRegion -> {
                        Region region = dbSubRegion.getRegion();
                        return (dbSubRegion.getName().equalsIgnoreCase(oldDto.getSubRegion())
                                && region.getName().equalsIgnoreCase(oldDto.getRegion()));
                    })
                    .findFirst()
                    .ifPresent(oldDto::setDbSubRegion);
        });

    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.SUB_REGIONS }, allEntries = true)
    public void saveAll(List<SubRegion> subRegions) {
        subRegionRepository.saveAll(subRegions);

    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.SUB_REGIONS }, allEntries = true)
    public ResponseDTO<String> delete(String code) {
        SubRegion subRegion = subRegionRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("Sub Region not found"));
        subRegionRepository.delete(subRegion);
        return new ResponseDTO<>("SUCCESS", "Sub Region deleted successfully");
    }

}
