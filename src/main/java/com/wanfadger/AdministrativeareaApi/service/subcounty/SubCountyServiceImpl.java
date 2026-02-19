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

import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.JoinType;
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

        if (subCountyRepository.existsByNameIgnoreCaseAndCounty_Code(dto.getName(), dto.getPartOfCode())) {
            throw new AlreadyExistsException("Sub county " + dto.getName() + " Already Exists in the county");
        }

        SubCounty subCounty = toSubCounty(dto, county);

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

        List<String> countyCodes = dtos.stream()
                .map(NewAdministrativeAreaDTO::getPartOfCode)
                .collect(Collectors.toList());

        Map<String, County> countyMap = countyRepository.findByCodeIgnoreCaseIn(countyCodes).stream()
                .collect(Collectors.toMap(County::getCode, c -> c, (existing, replacement) -> existing));

        List<SubCounty> subCounties = dtos.parallelStream()
                .filter(dto -> !subCountyRepository
                        .existsByNameIgnoreCaseAndCounty_Code(dto.getName(), dto.getPartOfCode()))
                .filter(dto -> countyMap.containsKey(dto.getPartOfCode()))
                .map(dto -> {
                    County county = countyMap.get(dto.getPartOfCode());
                    return toSubCounty(dto, county);
                })
                .toList();

        subCountyRepository.saveAll(subCounties);

        return new ResponseDTO<>("success",
                "successfully added " + subCounties.size() + " administrative areas");
    }

    private SubCounty toSubCounty(NewAdministrativeAreaDTO dto, County county) {
        return SubCounty.builder()
                .code(generateCode())
                .name(dto.getName().trim())
                .county(county)
                .latitude(dto.getLatitude() != null && !dto.getLatitude().isEmpty()
                        ? Double.valueOf(dto.getLatitude())
                        : null)
                .longitude(dto.getLongitude() != null && !dto.getLongitude().isEmpty()
                        ? Double.valueOf(dto.getLongitude())
                        : null)
                .description(dto.getDescription() != null && !dto.getDescription().isEmpty()
                        ? dto.getDescription().trim()
                        : null)
                .build();
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

    // @Override
    // @Cacheable(value = CacheValueKeyConfig.SUB_COUNTIES, key = "'list:' +
    // #countyCode")
    // public ResponseDTO<List<SubCountyDTO>> list(String countyCode) {
    // Specification<SubCounty> spec = Specification.where(null);
    // if (countyCode != null && !countyCode.isEmpty()) {
    // spec = spec
    // .and(new GenericSpecification<>(new SearchCriteria("county.code", countyCode,
    // MatchType.EQUALS)));
    // }

    // List<SubCountyDTO> subCountyDTOs = subCountyRepository.findAll(spec).stream()
    // .map(this::toDTO)
    // .sorted(Comparator.comparing(SubCountyDTO::getCode))
    // .toList();
    // return new ResponseDTO<>(subCountyDTOs);
    // }

    @Override
    @Cacheable(value = CacheValueKeyConfig.SUB_COUNTIES, key = "#code")
    public ResponseDTO<SubCountyDTO> getByCode(String code) {
        SubCounty subCounty = subCountyRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("SubCounty not found"));
        return new ResponseDTO<>(toDTO(subCounty));
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.SUB_COUNTIES, key = "#queryMap.toString()", unless = "#result.totalElements == 0")
    public PaginatedResponseDTO<SubCountyDTO> search(Map<String, String> queryMap) {
        // 1. Extract Pagination & Sorting
        int page = Optional.ofNullable(queryMap.get("page")).map(Integer::parseInt).orElse(1);
        int size = Optional.ofNullable(queryMap.get("size")).map(Integer::parseInt).orElse(10);
        String sortBy = Optional.ofNullable(queryMap.get("sortBy")).orElse("name");
        String sortDirection = Optional.ofNullable(queryMap.get("sortDirection")).orElse("ASC");

        page = page <= 0 ? 0 : page - 1;

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(sortDirection.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy));

        // 2. Build Specification
        Specification<SubCounty> spec = (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType()) {
                Fetch<SubCounty, County> countyFetch = root.fetch("county", JoinType.LEFT);
                Fetch<County, LocalGovernment> localGovernmentFetch = countyFetch.fetch("localGovernment",
                        JoinType.LEFT);
                Fetch<LocalGovernment, SubRegion> subRegionFetch = localGovernmentFetch.fetch("subRegion",
                        JoinType.LEFT);
                subRegionFetch.fetch("region", JoinType.LEFT);
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

        // 3. Execute Search
        Page<SubCounty> resultPage = subCountyRepository.findAll(spec, pageable);

        // 4. Map to DTOs
        List<SubCountyDTO> data = resultPage.getContent().stream()
                .map(this::toDTO)
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

    // @Override
    // public List<SubCounty> findAll() {
    // return subCountyRepository.findAll();
    // }

    // @Override
    // public List<SubCounty> findAllByCountyCode(String countyCode) {
    // return subCountyRepository
    // .findAll(new GenericSpecification<>(new SearchCriteria("county.code",
    // countyCode, MatchType.EQUALS)));
    // }

    // @Override
    // public List<SubCounty> findAllByCountyCodes(List<String> countyCodes) {
    // return subCountyRepository
    // .findAll(new GenericSpecification<>(new SearchCriteria("county.code",
    // countyCodes, MatchType.IN)));
    // }

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

    private SubCountyDTO toDTO(SubCounty subCounty) {
        SubCountyDTO dto = new SubCountyDTO();
        dto.setId(subCounty.getId());
        dto.setCode(subCounty.getCode());
        dto.setName(subCounty.getName());
        dto.setLatitude(subCounty.getLatitude() != null ? String.valueOf(subCounty.getLatitude()) : "");
        dto.setLongitude(subCounty.getLongitude() != null ? String.valueOf(subCounty.getLongitude()) : "");
        if (subCounty.getCounty() != null) {
            dto.setCounty(toCountyDTO(subCounty.getCounty()));
        }
        return dto;
    }

    private CountyDTO toCountyDTO(County county) {
        CountyDTO dto = new CountyDTO();
        dto.setCode(county.getCode());
        dto.setName(county.getName());
        dto.setLatitude(county.getLatitude() != null ? String.valueOf(county.getLatitude()) : "");
        dto.setLongitude(county.getLongitude() != null ? String.valueOf(county.getLongitude()) : "");
        dto.setLocalGovernment(toLocalGovernmentDTO(county.getLocalGovernment()));
        return dto;
    }

    private LocalGovernmentDTO toLocalGovernmentDTO(LocalGovernment localGovernment) {
        LocalGovernmentDTO dto = new LocalGovernmentDTO();
        dto.setCode(localGovernment.getCode());
        dto.setName(localGovernment.getName());
        dto.setLatitude(localGovernment.getLatitude() != null ? String.valueOf(localGovernment.getLatitude()) : "");
        dto.setLongitude(localGovernment.getLongitude() != null ? String.valueOf(localGovernment.getLongitude()) : "");
        dto.setSubRegion(toSubRegionDTO(localGovernment.getSubRegion()));
        return dto;
    }

    private SubRegionDTO toSubRegionDTO(SubRegion subRegion) {
        SubRegionDTO dto = new SubRegionDTO();
        dto.setCode(subRegion.getCode());
        dto.setName(subRegion.getName());
        dto.setLatitude(subRegion.getLatitude() != null ? String.valueOf(subRegion.getLatitude()) : "");
        dto.setLongitude(subRegion.getLongitude() != null ? String.valueOf(subRegion.getLongitude()) : "");
        dto.setRegion(toRegionDTO(subRegion.getRegion()));
        return dto;
    }

    private RegionDTO toRegionDTO(Region region) {
        RegionDTO dto = new RegionDTO();
        dto.setCode(region.getCode());
        dto.setName(region.getName());
        dto.setLongitude(region.getLongitude() != null ? String.valueOf(region.getLongitude()) : "");
        dto.setLatitude(region.getLatitude() != null ? String.valueOf(region.getLatitude()) : "");
        return dto;
    }
}
