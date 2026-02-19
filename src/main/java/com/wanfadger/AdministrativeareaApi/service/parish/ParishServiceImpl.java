package com.wanfadger.AdministrativeareaApi.service.parish;

import com.wanfadger.AdministrativeareaApi.areaexceptions.AlreadyExistsException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.MissingDataException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.NotFoundException;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaExcelDTO;
import com.wanfadger.AdministrativeareaApi.dto.CountyDTO;
import com.wanfadger.AdministrativeareaApi.dto.LocalGovernmentDTO;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.ParishDTO;
import com.wanfadger.AdministrativeareaApi.dto.RegionDTO;
import com.wanfadger.AdministrativeareaApi.dto.SubCountyDTO;
import com.wanfadger.AdministrativeareaApi.dto.SubRegionDTO;
import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.uniqueDtos.UParish;
import com.wanfadger.AdministrativeareaApi.entity.County;
import com.wanfadger.AdministrativeareaApi.entity.LocalGovernment;
import com.wanfadger.AdministrativeareaApi.entity.Parish;
import com.wanfadger.AdministrativeareaApi.entity.Region;
import com.wanfadger.AdministrativeareaApi.entity.SubCounty;
import com.wanfadger.AdministrativeareaApi.entity.SubRegion;
import com.wanfadger.AdministrativeareaApi.repository.ParishRepository;
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
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.JoinType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParishServiceImpl implements ParishService {

    private final ParishRepository parishRepository;
    private final SubCountyRepository subCountyRepository;
    private final SharedService sharedService;

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.PARISHES }, allEntries = true)
    public ResponseDTO<String> create(NewAdministrativeAreaDTO dto) {
        if (dto.getPartOfCode() == null || dto.getPartOfCode().isEmpty()) {
            throw new MissingDataException("Missing PartOfCode(sub county) for the parish");
        }

        SubCounty subCounty = subCountyRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                .orElseThrow(() -> new InvalidException("Invalid PartOfCode: " + dto.getPartOfCode()));

        if (parishRepository.existsByNameIgnoreCaseAndSubCounty_Code(dto.getName(), dto.getPartOfCode())) {
            throw new AlreadyExistsException("Parish " + dto.getName() + " Already Exists in the sub county");
        }

        Parish parish = toParish(dto, subCounty);

        parishRepository.save(parish);

        return new ResponseDTO<>(parish.getCode(), "successfully created a parish");
    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.PARISHES }, allEntries = true)
    public ResponseDTO<String> createAll(List<NewAdministrativeAreaDTO> dtos) {
        if (dtos.parallelStream().anyMatch(dto -> dto.getPartOfCode() == null || dto.getPartOfCode().isEmpty())) {
            throw new MissingDataException("Found Administrative Area without PartOfCoce");
        }

        List<String> subCountyCodes = dtos.stream()
                .map(NewAdministrativeAreaDTO::getPartOfCode)
                .collect(Collectors.toList());

        Map<String, SubCounty> subCountyMap = subCountyRepository.findByCodeIgnoreCaseIn(subCountyCodes).stream()
                .collect(Collectors.toMap(SubCounty::getCode, sc -> sc, (existing, replacement) -> existing));

        List<Parish> parishes = dtos.parallelStream()
                .filter(dto -> !parishRepository
                        .existsByNameIgnoreCaseAndSubCounty_Code(dto.getName(), dto.getPartOfCode()))
                .filter(dto -> subCountyMap.containsKey(dto.getPartOfCode()))
                .map(dto -> {
                    SubCounty subCounty = subCountyMap.get(dto.getPartOfCode());
                    return toParish(dto, subCounty);
                })
                .toList();

        parishRepository.saveAll(parishes);

        return new ResponseDTO<>("success",
                "successfully added " + parishes.size() + " administrative areas");
    }

    private Parish toParish(NewAdministrativeAreaDTO dto, SubCounty subCounty) {
        return Parish.builder()
                .code(generateCode())
                .name(dto.getName().trim())
                .subCounty(subCounty)
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
    @CacheEvict(value = { CacheValueKeyConfig.PARISHES }, allEntries = true)
    public ResponseDTO<String> update(String code, UpdateAdministrativeAreaDTO dto) {
        if (dto.getPartOfCode() == null || dto.getPartOfCode().isEmpty()) {
            throw new MissingDataException("Missing PartOfCode");
        }

        SubCounty subCounty = subCountyRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                .orElseThrow(() -> new InvalidException("Invalid PartOfCode"));
        Parish parish = parishRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("Administrative Area NotFound"));

        if (dto.getName() != null && !dto.getName().isEmpty()) {
            parish.setName(dto.getName());
        }

        if (dto.getLatitude() != null && !dto.getLatitude().isEmpty()) {
            parish.setLatitude(Double.valueOf(dto.getLatitude()));
        }

        if (dto.getLongitude() != null && !dto.getLongitude().isEmpty()) {
            parish.setLongitude(Double.valueOf(dto.getLongitude()));
        }

        if (dto.getDescription() != null && !dto.getDescription().isEmpty()) {
            parish.setDescription(dto.getDescription());
        }

        parish.setSubCounty(subCounty);

        parishRepository.save(parish);

        return new ResponseDTO<>("SUCCESS");
    }

    // @Override
    // @Cacheable(value = CacheValueKeyConfig.PARISHES, key = "'list:' +
    // #subCountyCode")
    // public ResponseDTO<List<ParishDTO>> list(String subCountyCode) {
    // Specification<Parish> spec = Specification.where(null);
    // if (subCountyCode != null && !subCountyCode.isEmpty()) {
    // spec = spec.and(
    // new GenericSpecification<>(new SearchCriteria("subCounty.code",
    // subCountyCode, MatchType.EQUALS)));
    // }

    // List<ParishDTO> parishDTOs = parishRepository.findAll(spec).stream()
    // .map(this::toDTO)
    // .sorted(Comparator.comparing(ParishDTO::getCode))
    // .toList();
    // return new ResponseDTO<>(parishDTOs);
    // }

    @Override
    @Cacheable(value = CacheValueKeyConfig.PARISHES, key = "#code")
    public ResponseDTO<ParishDTO> getByCode(String code) {
        Parish parish = parishRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("Parish not found"));
        return new ResponseDTO<>(toDTO(parish));
    }

    // @Override
    // @Cacheable(value = CacheValueKeyConfig.PARISHES, key = "'search:' + #name +
    // '-' + #code")
    // public ResponseDTO<List<ParishDTO>> search(String name, String code) {
    // Specification<Parish> spec = Specification.where(null);
    // if (name != null && !name.isEmpty()) {
    // spec = spec.and(new GenericSpecification<>(new SearchCriteria("name", name,
    // MatchType.CONTAINS)));
    // }
    // if (code != null && !code.isEmpty()) {
    // spec = spec.and(new GenericSpecification<>(new SearchCriteria("code", code,
    // MatchType.EQUALS)));
    // }

    // List<ParishDTO> parishDtos = parishRepository.findAll(spec).stream()
    // .map(this::toDTO)
    // .sorted(Comparator.comparing(ParishDTO::getCode))
    // .toList();

    // return new ResponseDTO<>(parishDtos);
    // }

    @Override
    @Cacheable(value = CacheValueKeyConfig.PARISHES, key = "#queryMap.toString()", unless = "#result.totalElements == 0")
    public PaginatedResponseDTO<ParishDTO> search(Map<String, String> queryMap) {
        // 1. Extract Pagination & Sorting
        int page = Optional.ofNullable(queryMap.get("page")).map(Integer::parseInt).orElse(1);
        int size = Optional.ofNullable(queryMap.get("size")).map(Integer::parseInt).orElse(10);
        String sortBy = Optional.ofNullable(queryMap.get("sortBy")).orElse("id");
        String sortDirection = Optional.ofNullable(queryMap.get("sortDirection")).orElse("ASC");

        page = page <= 0 ? 0 : page - 1;

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(sortDirection.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy));

        // 2. Build Specification
        Specification<Parish> spec = (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType()) {
                Fetch<Parish, SubCounty> subCountyFetch = root.fetch("subCounty", JoinType.LEFT);
                Fetch<SubCounty, County> countyFetch = subCountyFetch.fetch("county", JoinType.LEFT);
                Fetch<County, LocalGovernment> localGovernmentFetch = countyFetch.fetch("localGovernment",
                        JoinType.LEFT);
                Fetch<LocalGovernment, SubRegion> subRegionFetch = localGovernmentFetch.fetch("subRegion",
                        JoinType.LEFT);
                subRegionFetch.fetch("region", JoinType.LEFT);
            }
            return cb.conjunction();
        };

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
        Page<Parish> resultPage = parishRepository.findAll(spec, pageable);

        // 4. Map to DTOs
        List<ParishDTO> data = resultPage.getContent().stream()
                .map(this::toDTO)
                .toList();

        // 5. Build Paginated Response
        PaginatedResponseDTO<ParishDTO> response = new PaginatedResponseDTO<>();
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
    @CacheEvict(value = { CacheValueKeyConfig.PARISHES }, allEntries = true)
    public void upload(List<AdministrativeAreaExcelDTO> dtoList) {
        List<Parish> dbParishes = parishRepository.findAll();

        Set<UParish> newParishSet = dtoList.parallelStream()
                .filter(dto -> dbParishes.stream().parallel().noneMatch(dbParish -> {
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
                }))
                .map(dto -> new UParish(dto.getParish(), dto.getDbSubCounty().getId()))
                .collect(Collectors.toSet());

        if (newParishSet.size() > 0) {
            List<Parish> newParishes = newParishSet.stream().map(uP -> {
                Parish parish = new Parish();
                parish.setCode(generateCode());
                parish.setName(uP.getName());
                parish.setSubCounty(subCountyRepository.findById(uP.getId())
                        .orElseThrow(() -> new NotFoundException("SubCounty not found")));
                return parish;
            }).toList();
            parishRepository.saveAll(newParishes);
        }

    }

    @Override
    public Optional<Parish> findByCode(String code) {
        return parishRepository.findByCodeIgnoreCase(code);
    }

    // @Override
    // public List<Parish> findAll() {
    // return parishRepository.findAll();
    // }

    // @Override
    // public List<Parish> findAllBySubCountyCode(String subCountyCode) {
    // return parishRepository.findAll(
    // new GenericSpecification<>(new SearchCriteria("subCounty.code",
    // subCountyCode, MatchType.EQUALS)));
    // }

    // @Override
    // public List<Parish> findAllBySubCountyCodes(List<String> subCountyCodes) {
    // return parishRepository.findAll(
    // new GenericSpecification<>(new SearchCriteria("subCounty.code",
    // subCountyCodes, MatchType.IN)));
    // }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.PARISHES }, allEntries = true)
    public void saveAll(List<Parish> parishes) {
        parishRepository.saveAll(parishes);

    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.PARISHES }, allEntries = true)
    public ResponseDTO<String> delete(String code) {
        Parish parish = parishRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("Parish not found"));
        parishRepository.delete(parish);
        return new ResponseDTO<>("SUCCESS", "Parish deleted successfully");
    }

    private String generateCode() {
        return sharedService.generateCode(AdministrativeAreaType.PARISH);
    }

    private ParishDTO toDTO(Parish parish) {
        ParishDTO dto = new ParishDTO();
        dto.setId(parish.getId());
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
