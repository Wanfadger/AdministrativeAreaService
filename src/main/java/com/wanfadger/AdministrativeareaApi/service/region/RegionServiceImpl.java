package com.wanfadger.AdministrativeareaApi.service.region;

import com.wanfadger.AdministrativeareaApi.areaexceptions.AlreadyExistsException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.NotFoundException;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaExcelDTO;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.RegionDTO;
import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.entity.Region;
import com.wanfadger.AdministrativeareaApi.repository.RegionRepository;
import com.wanfadger.AdministrativeareaApi.shared.SharedService;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegionServiceImpl implements RegionService {

    private final RegionRepository regionRepository;
    private final SharedService sharedService;

    @Override
    @Transactional
    @CacheEvict(value = CacheValueKeyConfig.REGIONS, allEntries = true)
    public ResponseDTO<String> create(NewAdministrativeAreaDTO dto) {
        if (regionRepository.findByNameIgnoreCase(dto.getName()).isPresent()) {
            throw new AlreadyExistsException("Administrative Area Already Exists");
        }

        Region region = new Region();
        region.setCode(generateCode());
        region.setName(dto.getName());

        if (dto.getLatitude() != null && !dto.getLatitude().isEmpty()) {
            region.setLatitude(Double.valueOf(dto.getLatitude()));
        }
        if (dto.getLongitude() != null && !dto.getLongitude().isEmpty()) {
            region.setLongitude(Double.valueOf(dto.getLongitude()));
        }
        if (dto.getDescription() != null && !dto.getDescription().isEmpty()) {
            region.setDescription(dto.getDescription());
        }

        regionRepository.save(region);

        return new ResponseDTO<>(region.getCode(), "successfully created a region");
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheValueKeyConfig.REGIONS, allEntries = true)
    public ResponseDTO<String> createAll(List<NewAdministrativeAreaDTO> dtos) {
        List<Region> regions = dtos.parallelStream()
                .filter(dto -> regionRepository.findByNameIgnoreCase(dto.getName()).isEmpty())
                .map(this::convertDtoRegion)
                .toList();

        regionRepository.saveAll(Objects.requireNonNull(regions));

        return new ResponseDTO<>("success",
                "successfully added " + regions.size() + " administrative areas");
    }

    private Region convertDtoRegion(NewAdministrativeAreaDTO dto) {
        Region region = new Region();
        region.setName(dto.getName());
        region.setDescription(dto.getDescription());
        region.setLatitude(dto.getLatitude() != null ? Double.valueOf(dto.getLatitude()) : null);
        region.setLongitude(dto.getLongitude() != null ? Double.valueOf(dto.getLongitude()) : null);
        region.setCode(generateCode());
        return region;
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheValueKeyConfig.REGIONS, allEntries = true)
    public ResponseDTO<String> update(String code, UpdateAdministrativeAreaDTO dto) {
        Region region = regionRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new InvalidException("Invalid PartOfCode"));

        if (dto.getName() != null && !dto.getName().isEmpty()) {
            region.setName(dto.getName());
        }

        if (dto.getLatitude() != null && !dto.getLatitude().isEmpty()) {
            region.setLatitude(Double.valueOf(dto.getLatitude()));
        }

        if (dto.getLongitude() != null && !dto.getLongitude().isEmpty()) {
            region.setLongitude(Double.valueOf(dto.getLongitude()));
        }

        if (dto.getDescription() != null && !dto.getDescription().isEmpty()) {
            region.setDescription(dto.getDescription());
        }

        regionRepository.save(region);

        return new ResponseDTO<>("SUCCESS");
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.REGIONS, key = "'list'")
    public ResponseDTO<List<RegionDTO>> list() {
        List<RegionDTO> regionDtos = regionRepository.findAll().stream()
                .map(this::convertRegionDTO)
                .sorted(Comparator.comparing(RegionDTO::getCode))
                .toList();
        return new ResponseDTO<>(regionDtos);
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.REGIONS, key = "#code")
    public ResponseDTO<RegionDTO> getByCode(String code) {
        Region region = regionRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("Region not found"));
        return new ResponseDTO<>(convertRegionDTO(region));
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.REGIONS, key = "'search:' + #name + '-' + #code")
    public ResponseDTO<List<RegionDTO>> search(String name, String code) {
        Specification<Region> spec = Specification.where(null);
        if (name != null && !name.isEmpty()) {
            spec = spec.and(new GenericSpecification<>(new SearchCriteria("name", name, MatchType.CONTAINS)));
        }
        if (code != null && !code.isEmpty()) {
            spec = spec.and(new GenericSpecification<>(new SearchCriteria("code", code, MatchType.EQUALS)));
        }

        List<RegionDTO> regionDtos = regionRepository.findAll(spec).stream()
                .map(this::convertRegionDTO)
                .sorted(Comparator.comparing(RegionDTO::getCode))
                .toList();

        return new ResponseDTO<>(regionDtos);
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.REGIONS, key = "#queryMap.toString()", unless = "#result.totalElements == 0")
    public PaginatedResponseDTO<RegionDTO> search(Map<String, String> queryMap) {
        // 1. Extract Pagination & Sorting
        int page = Optional.ofNullable(queryMap.get("page")).map(Integer::parseInt).orElse(1);
        int size = Optional.ofNullable(queryMap.get("size")).map(Integer::parseInt).orElse(10);
        String sortBy = Optional.ofNullable(queryMap.get("sortBy")).orElse("id");
        String sortDirection = Optional.ofNullable(queryMap.get("sortDirection")).orElse("ASC");

        page = page <= 0 ? 0 : page - 1;

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(sortDirection.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy));

        // 2. Build Specification
        Specification<Region> spec = Specification.where(null);
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
                .map(this::convertRegionDTO)
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
    @CacheEvict(value = CacheValueKeyConfig.REGIONS, allEntries = true)
    public void upload(List<AdministrativeAreaExcelDTO> dtoList) {
        // Step 1: Get all existing regions from database
        List<Region> dbRegions = regionRepository.findAll();

        // Step 2: Extract unique new regions from Excel (exclude existing ones)
        List<Region> newRegions = dtoList.parallelStream()
                .filter(dto -> dbRegions.stream()
                        .noneMatch(dbRegion -> dbRegion.getName().equalsIgnoreCase(dto.getRegion())))
                .filter(distinctByKey(AdministrativeAreaExcelDTO::getRegion))
                .map(dto -> {
                    Region region = new Region();
                    region.setCode(generateCode());
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

        // Step 4: Update DTOs with DB region references
        dtoList.parallelStream().forEach(dto -> {
            dbRegions2.stream()
                    .filter(r -> r.getName().equalsIgnoreCase(dto.getRegion()))
                    .findFirst()
                    .ifPresent(dto::setDbRegion);
        });

    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        ConcurrentHashMap<Object, Boolean> map = new ConcurrentHashMap<>();
        return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    @Override
    public Optional<Region> findByCode(String code) {
        return regionRepository.findByCodeIgnoreCase(code);
    }

    @Override
    public List<Region> findAll() {
        return regionRepository.findAll();
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheValueKeyConfig.REGIONS, allEntries = true)
    public void saveAll(List<Region> regions) {
        regionRepository.saveAll(regions);

    }

    @Override
    @Transactional
    @CacheEvict(value = CacheValueKeyConfig.REGIONS, allEntries = true)
    public ResponseDTO<String> delete(String code) {
        Region region = regionRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("Region not found"));
        regionRepository.delete(region);
        return new ResponseDTO<>("SUCCESS", "Region deleted successfully");
    }

    private String generateCode() {
        return sharedService.generateCode(AdministrativeAreaType.REGION);
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
