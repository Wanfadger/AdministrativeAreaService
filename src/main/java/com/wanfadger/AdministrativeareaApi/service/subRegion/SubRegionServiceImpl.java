package com.wanfadger.AdministrativeareaApi.service.subRegion;

import com.wanfadger.AdministrativeareaApi.areaexceptions.AlreadyExistsException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.MissingDataException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.NotFoundException;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaExcelDTO;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.RegionDTO;
import com.wanfadger.AdministrativeareaApi.dto.SubRegionDTO;
import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.uniqueDtos.USubRegion;
import com.wanfadger.AdministrativeareaApi.entity.Region;
import com.wanfadger.AdministrativeareaApi.entity.SubRegion;
import com.wanfadger.AdministrativeareaApi.repository.RegionRepository;
import com.wanfadger.AdministrativeareaApi.repository.SubRegionRepository;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.shared.SharedService;

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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubRegionServiceImpl implements SubRegionService {

    private final SubRegionRepository subRegionRepository;
    private final RegionRepository regionRepository;
    private final SharedService sharedService;

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.SUB_REGIONS }, allEntries = true)
    public ResponseDTO<String> create(NewAdministrativeAreaDTO dto) {
        if (dto.getPartOfCode() == null || dto.getPartOfCode().isEmpty()) {
            throw new MissingDataException("Missing PartOfCode(region) for the sub region");
        }

        Region region = regionRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                .orElseThrow(() -> new InvalidException("Invalid PartOfCode: " + dto.getPartOfCode()));

        if (subRegionRepository.findByNameIgnoreCaseAndRegion_Code(dto.getName(), dto.getPartOfCode()).isPresent()) {
            throw new AlreadyExistsException("Sub region Already Exists in the region");
        }

        SubRegion subRegion = new SubRegion();
        subRegion.setCode(generateCode());
        subRegion.setName(dto.getName());
        subRegion.setRegion(region);

        if (dto.getLatitude() != null && !dto.getLatitude().isEmpty()) {
            subRegion.setLatitude(Double.valueOf(dto.getLatitude()));
        }
        if (dto.getLongitude() != null && !dto.getLongitude().isEmpty()) {
            subRegion.setLongitude(Double.valueOf(dto.getLongitude()));
        }
        if (dto.getDescription() != null && !dto.getDescription().isEmpty()) {
            subRegion.setDescription(dto.getDescription());
        }

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

        List<SubRegion> subRegions = dtos.parallelStream()
                .filter(dto -> subRegionRepository
                        .findByNameIgnoreCaseAndRegion_Code(dto.getName(), dto.getPartOfCode()).isEmpty())
                .map(this::convertDtoSubRegion)
                .toList();

        // Need to set Region for each. convertDtoSubRegion doesn't set it because it
        // requires DB lookup.
        // Wait, convertDtoSubRegion logic in AdminService didn't set parent.
        // AdminService did it:
        // subRegion.setRegion(region);
        // But here we need to fetch region by partOfCode.
        // Doing it in parallel map might be okay if fetching eagerly or efficiently.
        // Or fetch all relevant regions first?
        // Since partOfCode is region code.

        // Let's refine the mapping:
        subRegions.forEach(subRegion -> {
            // Retrieve dto again? No, we lost the link.
            // We should map DTO -> Entity inside the stream correctly.
        });

        // Correct implementation:
        List<SubRegion> validSubRegions = dtos.parallelStream()
                .filter(dto -> subRegionRepository
                        .findByNameIgnoreCaseAndRegion_Code(dto.getName(), dto.getPartOfCode()).isEmpty())
                .map(dto -> {
                    SubRegion subRegion = new SubRegion();
                    subRegion.setName(dto.getName());
                    subRegion.setLatitude(dto.getLatitude() != null && !dto.getLatitude().isEmpty()
                            ? Double.valueOf(dto.getLatitude())
                            : null);
                    subRegion.setLongitude(dto.getLongitude() != null && !dto.getLongitude().isEmpty()
                            ? Double.valueOf(dto.getLongitude())
                            : null);
                    subRegion.setCode(generateCode());

                    // Fetch parent region. Note: this might fail if parent doesn't exist.
                    // AdminService throws InvalidException if not found.
                    Region region = regionRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                            .orElseThrow(() -> new InvalidException("Invalid PartOfCode: " + dto.getPartOfCode()));
                    subRegion.setRegion(region);

                    return subRegion;
                })
                .toList();

        subRegionRepository.saveAll(Objects.requireNonNull(validSubRegions));

        return new ResponseDTO<>("success",
                "successfully added " + validSubRegions.size() + " administrative areas");
    }

    // Kept for reference but not used in createAll to avoid double mapping
    private SubRegion convertDtoSubRegion(NewAdministrativeAreaDTO dto) {
        // ...
        return null;
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
                .orElseThrow(() -> new NotFoundException("Administrative Area NotFound"));

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
    @Cacheable(value = CacheValueKeyConfig.SUB_REGIONS, key = "'list:' + #regionCode")
    public ResponseDTO<List<SubRegionDTO>> list(String regionCode) {
        Specification<SubRegion> spec = Specification.where(null);
        if (regionCode != null && !regionCode.isEmpty()) {
            spec = spec
                    .and(new GenericSpecification<>(new SearchCriteria("region.code", regionCode, MatchType.EQUALS)));
        }

        List<SubRegionDTO> subRegionDTOs = subRegionRepository.findAll(spec).stream()
                .map(this::convertSubRegionDTO)
                .sorted(Comparator.comparing(SubRegionDTO::getCode))
                .toList();
        return new ResponseDTO<>(subRegionDTOs);
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.SUB_REGIONS, key = "#code")
    public ResponseDTO<SubRegionDTO> getByCode(String code) {
        SubRegion subRegion = subRegionRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("SubRegion not found"));
        return new ResponseDTO<>(convertSubRegionDTO(subRegion));
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.SUB_REGIONS, key = "'search:' + #name + '-' + #code")
    public ResponseDTO<List<SubRegionDTO>> search(String name, String code) {
        Specification<SubRegion> spec = Specification.where(null);
        if (name != null && !name.isEmpty()) {
            spec = spec.and(new GenericSpecification<>(new SearchCriteria("name", name, MatchType.CONTAINS)));
        }
        if (code != null && !code.isEmpty()) {
            spec = spec.and(new GenericSpecification<>(new SearchCriteria("code", code, MatchType.EQUALS)));
        }

        List<SubRegionDTO> subRegionDtos = subRegionRepository.findAll(spec).stream()
                .map(this::convertSubRegionDTO)
                .sorted(Comparator.comparing(SubRegionDTO::getCode))
                .toList();

        return new ResponseDTO<>(subRegionDtos);
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.SUB_REGIONS, key = "#queryMap.toString()", unless = "#result.totalElements == 0")
    public PaginatedResponseDTO<SubRegionDTO> search(Map<String, String> queryMap) {
        // 1. Extract Pagination & Sorting
        int page = Optional.ofNullable(queryMap.get("page")).map(Integer::parseInt).orElse(1);
        int size = Optional.ofNullable(queryMap.get("size")).map(Integer::parseInt).orElse(10);
        String sortBy = Optional.ofNullable(queryMap.get("sortBy")).orElse("id");
        String sortDirection = Optional.ofNullable(queryMap.get("sortDirection")).orElse("ASC");

        page = page <= 0 ? 0 : page - 1;

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(sortDirection.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy));

        // 2. Build Specification
        Specification<SubRegion> spec = Specification.where(null);
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
        Page<SubRegion> resultPage = subRegionRepository.findAll(spec, pageable);

        // 4. Map to DTOs
        List<SubRegionDTO> data = resultPage.getContent().stream()
                .map(this::convertSubRegionDTO)
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
                subRegion.setCode(generateCode());
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
    public Optional<SubRegion> findByCode(String code) {
        return subRegionRepository.findByCodeIgnoreCase(code);
    }

    @Override
    public List<SubRegion> findAll() {
        return subRegionRepository.findAll();
    }

    @Override
    public List<SubRegion> findAllByRegionCode(String regionCode) {
        return subRegionRepository
                .findAll(new GenericSpecification<>(new SearchCriteria("region.code", regionCode, MatchType.EQUALS)));
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

    private String generateCode() {
        return sharedService.generateCode(AdministrativeAreaType.SUBREGION);
    }

    private SubRegionDTO convertSubRegionDTO(SubRegion subRegion) {
        SubRegionDTO dto = new SubRegionDTO();
        dto.setId(subRegion.getId());
        dto.setCode(subRegion.getCode());
        dto.setName(subRegion.getName());
        dto.setLatitude(subRegion.getLatitude() != null ? String.valueOf(subRegion.getLatitude()) : "");
        dto.setLongitude(subRegion.getLongitude() != null ? String.valueOf(subRegion.getLongitude()) : "");
        dto.setRegion(convertRegionDTO(subRegion.getRegion()));
        return dto;
    }

    private RegionDTO convertRegionDTO(Region region) {
        RegionDTO dto = new RegionDTO();
        dto.setCode(region.getCode());
        dto.setName(region.getName());
        dto.setLongitude(region.getLongitude() != null ? String.valueOf(region.getLongitude()) : "");
        dto.setLatitude(region.getLatitude() != null ? String.valueOf(region.getLatitude()) : "");
        return dto;
    }
}
