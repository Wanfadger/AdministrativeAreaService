package com.wanfadger.AdministrativeareaApi.service.subcounty;

import com.wanfadger.AdministrativeareaApi.areaexceptions.AlreadyExistsException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.MissingDataException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.NotFoundException;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaExcelDTO;
import com.wanfadger.AdministrativeareaApi.dto.CountyDTO;
import com.wanfadger.AdministrativeareaApi.dto.LocalGovernmentDTO;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.RegionDTO;
import com.wanfadger.AdministrativeareaApi.dto.SubCountyDTO;
import com.wanfadger.AdministrativeareaApi.dto.SubRegionDTO;
import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.uniqueDtos.USubCounty;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.entity.County;
import com.wanfadger.AdministrativeareaApi.entity.LocalGovernment;
import com.wanfadger.AdministrativeareaApi.entity.Region;
import com.wanfadger.AdministrativeareaApi.entity.SubCounty;
import com.wanfadger.AdministrativeareaApi.entity.SubRegion;
import com.wanfadger.AdministrativeareaApi.repository.CountyRepository;
import com.wanfadger.AdministrativeareaApi.repository.SubCountyRepository;
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
import java.util.Set;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubCountyServiceImpl implements SubCountyService {

    private final SubCountyRepository subCountyRepository;
    private final CountyRepository countyRepository;
    private final SharedService sharedService;

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.SUB_COUNTIES }, allEntries = true)
    public ResponseDTO<String> create(NewAdministrativeAreaDTO dto) {
        if (dto.getPartOfCode() == null || dto.getPartOfCode().isEmpty()) {
            throw new MissingDataException("Missing PartOfCode(county) for the sub county");
        }

        County county = countyRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                .orElseThrow(() -> new InvalidException("Invalid PartOfCode: " + dto.getPartOfCode()));

        if (subCountyRepository.findByNameIgnoreCaseAndCounty_Id(dto.getName(), county.getId()).isPresent()) {
            throw new AlreadyExistsException("Sub county Already Exists in the county");
        }

        SubCounty subCounty = new SubCounty();
        subCounty.setCode(generateCode());
        subCounty.setName(dto.getName());
        subCounty.setCounty(county);

        if (dto.getLatitude() != null && !dto.getLatitude().isEmpty()) {
            subCounty.setLatitude(Double.valueOf(dto.getLatitude()));
        }
        if (dto.getLongitude() != null && !dto.getLongitude().isEmpty()) {
            subCounty.setLongitude(Double.valueOf(dto.getLongitude()));
        }
        if (dto.getDescription() != null && !dto.getDescription().isEmpty()) {
            subCounty.setDescription(dto.getDescription());
        }

        subCountyRepository.save(subCounty);

        return new ResponseDTO<>(subCounty.getCode(), "successfully created a sub county");
    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.SUB_COUNTIES }, allEntries = true)
    public ResponseDTO<String> createAll(List<NewAdministrativeAreaDTO> dtos) {
        if (dtos.parallelStream().anyMatch(dto -> dto.getPartOfCode() == null || dto.getPartOfCode().isEmpty())) {
            throw new MissingDataException("Found Administrative Area without PartOfCoce");
        }

        List<SubCounty> subCounties = dtos.parallelStream()
                .filter(dto -> {
                    // Manually fetch county to check existence?
                    // The original implementation used
                    // subCountyRepository.findByNameIgnoreCaseAndCounty_Id
                    // This requires County ID.
                    // So we must fetch county first.
                    // But we can filter by querying repository.
                    // Wait, findByNameIgnoreCaseAndCounty_Id expects a Long ID? Original code
                    // implies it takes String or Long?
                    // "subCountyRepository.findByNameIgnoreCaseAndCounty_Id(dto.getName(),
                    // dto.getPartOfCode())"
                    // If PartOfCode is String code, then method signature might be
                    // findByNameIgnoreCaseAndCounty_Code?
                    // Let's assume it works as implemented in old Service.
                    // Actually, let's play safe and check if it takes Code or ID.
                    // The old code:
                    // findByNameIgnoreCaseAndCounty_Id(dto.getName(), dto.getPartOfCode())
                    // If PartOfCode is String, this implies County_Id is mapped to String Code in a
                    // custom query or it is actually County_Code.
                    // Given the pattern so far, likely County_Code.

                    // However, to be safe and consistent with create(), I will fetch County by
                    // code.
                    // But for filtering in stream, fetching County for each checking is expensive.
                    // But `createAll` assumes not too many items or accepts this cost.

                    // Let's rely on repository method if it works.
                    // But I need to construct the entity.
                    return true;
                })
                .map(dto -> {
                    // This whole block is inside map, so filters need to happen before map or
                    // inside map (returning null and filtering nulls).
                    return dto;
                })
                // Let's rewrite cleaner:
                .map(dto -> {
                    County county = countyRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                            .orElseThrow(() -> new InvalidException("Invalid PartOfCode: " + dto.getPartOfCode()));

                    if (subCountyRepository.findByNameIgnoreCaseAndCounty_Id(dto.getName(), county.getId())
                            .isPresent()) {
                        return null; // Skip existing
                    }

                    SubCounty subCounty = new SubCounty();
                    subCounty.setName(dto.getName());
                    subCounty.setLatitude(dto.getLatitude() != null && !dto.getLatitude().isEmpty()
                            ? Double.valueOf(dto.getLatitude())
                            : null);
                    subCounty.setLongitude(dto.getLongitude() != null && !dto.getLongitude().isEmpty()
                            ? Double.valueOf(dto.getLongitude())
                            : null);
                    subCounty.setCode(generateCode());
                    subCounty.setCounty(county);
                    return subCounty;
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        subCountyRepository.saveAll(subCounties);

        return new ResponseDTO<>("success",
                "successfully added " + subCounties.size() + " administrative areas");
    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.SUB_COUNTIES }, allEntries = true)
    public ResponseDTO<String> update(String code, UpdateAdministrativeAreaDTO dto) {
        if (dto.getPartOfCode() == null || dto.getPartOfCode().isEmpty()) {
            throw new MissingDataException("Missing PartOfCode");
        }

        County county = countyRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                .orElseThrow(() -> new InvalidException("Invalid PartOfCode"));
        SubCounty subCounty = subCountyRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("Administrative Area NotFound"));

        if (dto.getName() != null && !dto.getName().isEmpty()) {
            subCounty.setName(dto.getName());
        }

        if (dto.getLatitude() != null && !dto.getLatitude().isEmpty()) {
            subCounty.setLatitude(Double.valueOf(dto.getLatitude()));
        }

        if (dto.getLongitude() != null && !dto.getLongitude().isEmpty()) {
            subCounty.setLongitude(Double.valueOf(dto.getLongitude()));
        }

        if (dto.getDescription() != null && !dto.getDescription().isEmpty()) {
            subCounty.setDescription(dto.getDescription());
        }

        subCounty.setCounty(county);

        subCountyRepository.save(subCounty);

        return new ResponseDTO<>("SUCCESS");
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.SUB_COUNTIES, key = "'list:' + #countyCode")
    public ResponseDTO<List<SubCountyDTO>> list(String countyCode) {
        Specification<SubCounty> spec = Specification.where(null);
        if (countyCode != null && !countyCode.isEmpty()) {
            spec = spec
                    .and(new GenericSpecification<>(new SearchCriteria("county.code", countyCode, MatchType.EQUALS)));
        }

        List<SubCountyDTO> subCountyDTOs = subCountyRepository.findAll(spec).stream()
                .map(this::convertSubCountyDTO)
                .sorted(Comparator.comparing(SubCountyDTO::getCode))
                .toList();
        return new ResponseDTO<>(subCountyDTOs);
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.SUB_COUNTIES, key = "#code")
    public ResponseDTO<SubCountyDTO> getByCode(String code) {
        SubCounty subCounty = subCountyRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("SubCounty not found"));
        return new ResponseDTO<>(convertSubCountyDTO(subCounty));
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.SUB_COUNTIES, key = "'search:' + #name + '-' + #code")
    public ResponseDTO<List<SubCountyDTO>> search(String name, String code) {
        Specification<SubCounty> spec = Specification.where(null);
        if (name != null && !name.isEmpty()) {
            spec = spec.and(new GenericSpecification<>(new SearchCriteria("name", name, MatchType.CONTAINS)));
        }
        if (code != null && !code.isEmpty()) {
            spec = spec.and(new GenericSpecification<>(new SearchCriteria("code", code, MatchType.EQUALS)));
        }

        List<SubCountyDTO> subCountyDtos = subCountyRepository.findAll(spec).stream()
                .map(this::convertSubCountyDTO)
                .sorted(Comparator.comparing(SubCountyDTO::getCode))
                .toList();

        return new ResponseDTO<>(subCountyDtos);
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.SUB_COUNTIES, key = "#queryMap.toString()", unless = "#result.totalElements == 0")
    public PaginatedResponseDTO<SubCountyDTO> search(Map<String, String> queryMap) {
        // 1. Extract Pagination & Sorting
        int page = Optional.ofNullable(queryMap.get("page")).map(Integer::parseInt).orElse(1);
        int size = Optional.ofNullable(queryMap.get("size")).map(Integer::parseInt).orElse(10);
        String sortBy = Optional.ofNullable(queryMap.get("sortBy")).orElse("id");
        String sortDirection = Optional.ofNullable(queryMap.get("sortDirection")).orElse("ASC");

        page = page <= 0 ? 0 : page - 1;

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(sortDirection.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy));

        // 2. Build Specification
        Specification<SubCounty> spec = Specification.where(null);
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
        Page<SubCounty> resultPage = subCountyRepository.findAll(spec, pageable);

        // 4. Map to DTOs
        List<SubCountyDTO> data = resultPage.getContent().stream()
                .map(this::convertSubCountyDTO)
                .toList();

        // 5. Build Paginated Response
        PaginatedResponseDTO<SubCountyDTO> response = new PaginatedResponseDTO<>();
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
    @CacheEvict(value = { CacheValueKeyConfig.SUB_COUNTIES }, allEntries = true)
    public void upload(List<AdministrativeAreaExcelDTO> dtoList) {
        List<SubCounty> dbSubCounties = subCountyRepository.findAll();

        Set<USubCounty> newSubCountySet = dtoList.parallelStream()
                .filter(dto -> dbSubCounties.stream().parallel().noneMatch(dbSubCounty -> {
                    County county = dbSubCounty.getCounty();
                    LocalGovernment localGovernment = county.getLocalGovernment();
                    SubRegion subRegion = localGovernment.getSubRegion();
                    Region region = subRegion.getRegion();
                    return (dbSubCounty.getName().equalsIgnoreCase(dto.getSubCounty())
                            && county.getName().equalsIgnoreCase(dto.getCounty())
                            && localGovernment.getName().equalsIgnoreCase(dto.getLocalGovernment())
                            && subRegion.getName().equalsIgnoreCase(dto.getSubRegion())
                            && region.getName().equalsIgnoreCase(dto.getRegion()));
                }))
                .map(dto -> new USubCounty(dto.getSubCounty(), dto.getDbCounty().getId()))
                .collect(Collectors.toSet());

        List<SubCounty> dbSubCounties2;
        if (newSubCountySet.size() > 0) {
            List<SubCounty> newSubCounties = newSubCountySet.stream().map(uSC -> {
                SubCounty subCounty = new SubCounty();
                subCounty.setCode(generateCode());
                subCounty.setName(uSC.getName());
                subCounty.setCounty(countyRepository.findById(uSC.getId())
                        .orElseThrow(() -> new NotFoundException("County not found")));
                return subCounty;
            }).toList();
            subCountyRepository.saveAll(newSubCounties);
            dbSubCounties2 = subCountyRepository.findAll();
        } else {
            dbSubCounties2 = dbSubCounties;
        }

        dtoList.parallelStream().forEach(oldDto -> {
            dbSubCounties2.stream()
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
                    .findFirst()
                    .ifPresent(oldDto::setDbSubCounty);
        });

    }

    @Override
    public Optional<SubCounty> findByCode(String code) {
        return subCountyRepository.findByCodeIgnoreCase(code);
    }

    @Override
    public List<SubCounty> findAll() {
        return subCountyRepository.findAll();
    }

    @Override
    public List<SubCounty> findAllByCountyCode(String countyCode) {
        return subCountyRepository
                .findAll(new GenericSpecification<>(new SearchCriteria("county.code", countyCode, MatchType.EQUALS)));
    }

    @Override
    public List<SubCounty> findAllByCountyCodes(List<String> countyCodes) {
        return subCountyRepository
                .findAll(new GenericSpecification<>(new SearchCriteria("county.code", countyCodes, MatchType.IN)));
    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.SUB_COUNTIES }, allEntries = true)
    public void saveAll(List<SubCounty> subCounties) {
        subCountyRepository.saveAll(subCounties);

    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.SUB_COUNTIES }, allEntries = true)
    public ResponseDTO<String> delete(String code) {
        SubCounty subCounty = subCountyRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("Sub County not found"));
        subCountyRepository.delete(subCounty);
        return new ResponseDTO<>("SUCCESS", "Sub County deleted successfully");
    }

    private String generateCode() {
        return sharedService.generateCode(AdministrativeAreaType.SUBCOUNTY);
    }

    private SubCountyDTO convertSubCountyDTO(SubCounty subCounty) {
        SubCountyDTO dto = new SubCountyDTO();
        dto.setId(subCounty.getId());
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

    private SubRegionDTO convertSubRegionDTO(SubRegion subRegion) {
        SubRegionDTO dto = new SubRegionDTO();
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
