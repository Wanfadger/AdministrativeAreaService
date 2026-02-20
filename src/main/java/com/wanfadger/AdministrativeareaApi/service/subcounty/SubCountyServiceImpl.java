package com.wanfadger.AdministrativeareaApi.service.subcounty;

import com.wanfadger.AdministrativeareaApi.areaexceptions.AlreadyExistsException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.MissingDataException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.NotFoundException;
import com.wanfadger.AdministrativeareaApi.dto.ExcelJsonDTO;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.SubCountyDTO;

import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.entity.County;
import com.wanfadger.AdministrativeareaApi.entity.LocalGovernment;

import com.wanfadger.AdministrativeareaApi.entity.SubCounty;
import com.wanfadger.AdministrativeareaApi.entity.SubRegion;
import com.wanfadger.AdministrativeareaApi.repository.CountyRepository;
import com.wanfadger.AdministrativeareaApi.repository.SubCountyRepository;
import com.wanfadger.AdministrativeareaApi.repository.ParishRepository;
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
    private final SubCountyMapperService subCountyMapperService;

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.SUB_COUNTIES }, allEntries = true)
    public ResponseDTO<String> create(NewAdministrativeAreaDTO dto) {
        if (dto.getParentCode() == null || dto.getParentCode().isEmpty()) {
            throw new MissingDataException("Missing PartOfCode(county) for the sub county");
        }

        County county = countyRepository.findByCodeIgnoreCase(dto.getParentCode())
                .orElseThrow(() -> new InvalidException("Invalid PartOfCode: " + dto.getParentCode()));

        if (subCountyRepository.existsByNameIgnoreCaseAndCounty_Code(dto.getName(), dto.getParentCode())) {
            throw new AlreadyExistsException("Sub county " + dto.getName() + " Already Exists in the county");
        }

        SubCounty subCounty = subCountyMapperService.toSubCounty(dto, county);

        subCountyRepository.save(subCounty);

        return new ResponseDTO<>(subCounty.getCode(), "successfully created a sub county");
    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.SUB_COUNTIES }, allEntries = true)
    public ResponseDTO<String> createAll(List<NewAdministrativeAreaDTO> dtos) {
        if (dtos.parallelStream().anyMatch(dto -> dto.getParentCode() == null || dto.getParentCode().isEmpty())) {
            throw new MissingDataException("Found Administrative Area without PartOfCoce");
        }

        List<String> countyCodes = dtos.stream()
                .map(NewAdministrativeAreaDTO::getParentCode)
                .collect(Collectors.toList());

        Map<String, County> countyMap = countyRepository.findByCodeIgnoreCaseIn(countyCodes).stream()
                .collect(Collectors.toMap(County::getCode, c -> c, (existing, replacement) -> existing));

        List<SubCounty> subCounties = dtos.parallelStream()
                .filter(dto -> !subCountyRepository
                        .existsByNameIgnoreCaseAndCounty_Code(dto.getName(), dto.getParentCode()))
                .filter(dto -> countyMap.containsKey(dto.getParentCode()))
                .map(dto -> {
                    County county = countyMap.get(dto.getParentCode());
                    return subCountyMapperService.toSubCounty(dto, county);
                })
                .toList();

        subCountyRepository.saveAll(subCounties);

        return new ResponseDTO<>("success",
                "successfully added " + subCounties.size() + " administrative areas");
    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.SUB_COUNTIES,
            CacheValueKeyConfig.SUB_COUNTIES_FILTERED }, allEntries = true)
    public ResponseDTO<String> update(UpdateAdministrativeAreaDTO dto) {
        if (dto.getParentCode() == null || dto.getParentCode().isEmpty()) {
            throw new MissingDataException("Missing parent code");
        }

        County county = countyRepository.findByCodeIgnoreCase(dto.getParentCode())
                .orElseThrow(() -> new NotFoundException("County with code " + dto.getParentCode() + " not found"));
        SubCounty subCounty = subCountyRepository.findByCodeIgnoreCaseAndCounty_Code(dto.getCode(), dto.getParentCode())
                .orElseThrow(() -> new NotFoundException(
                        "Sub county with code " + dto.getCode() + " not found in county " + county.getName()));

        if (dto.getName() != null && !dto.getName().isEmpty() && !subCounty.getName().equalsIgnoreCase(dto.getName())) {
            if (subCountyRepository.existsByNameIgnoreCaseAndCounty_Code(dto.getName(), dto.getParentCode())) {
                throw new AlreadyExistsException(
                        "Sub county " + dto.getName() + " Already Exists in the " + county.getName() + " county");
            }
            subCounty.setName(dto.getName());
        }

        if (dto.getLatitude() != null && !dto.getLatitude().isEmpty()
                && !subCounty.getLatitude().toString().equalsIgnoreCase(dto.getLatitude())) {
            subCounty.setLatitude(Double.valueOf(dto.getLatitude()));
        }

        if (dto.getLongitude() != null && !dto.getLongitude().isEmpty()
                && !subCounty.getLongitude().toString().equalsIgnoreCase(dto.getLongitude())) {
            subCounty.setLongitude(Double.valueOf(dto.getLongitude()));
        }

        if (dto.getDescription() != null && !dto.getDescription().isEmpty()
                && !subCounty.getDescription().equalsIgnoreCase(dto.getDescription())) {
            subCounty.setDescription(dto.getDescription());
        }

        subCountyRepository.save(subCounty);

        return new ResponseDTO<>(subCounty.getCode(), "successfully updated sub county");
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.SUB_COUNTIES, key = "#code")
    public ResponseDTO<SubCountyDTO> getByCode(String code) {
        SubCounty subCounty = subCountyRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("SubCounty not found"));
        return new ResponseDTO<>(subCountyMapperService.toDTO(subCounty));
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.SUB_COUNTIES, key = "#code+'_details'")
    public ResponseDTO<SubCountyDTO> findDetailsByCode(String code) {
        SubCounty subCounty = subCountyRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("SubCounty not found"));
        return new ResponseDTO<>(subCountyMapperService.toDTO(subCounty));
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.SUB_COUNTIES_FILTERED, keyGenerator = "sortedMapKeyGenerator", unless = "#result.totalElements == 0")
    @Transactional(readOnly = true)
    public PaginatedResponseDTO<SubCountyDTO> filter(Map<String, String> queryMap) {
        // 1. Extract Pagination & Sorting
        int page = Optional.ofNullable(queryMap.get("page")).map(Integer::parseInt).orElse(1);
        int size = Optional.ofNullable(queryMap.get("size")).map(Integer::parseInt).orElse(10);
        String sortBy = Optional.ofNullable(queryMap.get("sortBy")).orElse("name");
        String sortDirection = Optional.ofNullable(queryMap.get("sortDirection")).orElse("ASC");

        page = page <= 0 ? 0 : page - 1;

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(sortDirection.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy));

        // 2. Build Specification, including generic filtering
        Specification<SubCounty> spec = new GenericSpecification<>(
                new SearchCriteria("archived", "false", MatchType.EQUALS));
        for (Map.Entry<String, String> entry : queryMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (List.of("page", "size", "sortBy", "sortDirection", "type", "selected").contains(key))
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

        if (queryMap.containsKey("selected") && queryMap.get("selected") != null
                && !queryMap.get("selected").isEmpty()) {
            spec = spec.and(new GenericSpecification<>(
                    new SearchCriteria("county.code", queryMap.get("selected"), MatchType.EQUALS)));
        }

        // 3. Execute Search
        Page<SubCounty> resultPage = subCountyRepository.findAll(spec, pageable);

        // 4. Map to DTOs
        List<SubCountyDTO> data = resultPage.getContent().stream()
                .map(subCountyMapperService::toDTO)
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
                .map(subCountyMapperService::toDetailDTO) // Changed to toDetailDTO
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
    public List<SubCounty> upload(List<ExcelJsonDTO> dtoList, Map<String, County> countyMap) {
        // filter unique sub counties in each county
        List<SubCounty> uniqueSubCounties = dtoList.stream()
                .filter(e -> e.getRegion() != null && e.getSubRegion() != null && e.getLocalGovernment() != null
                        && e.getCounty() != null && e.getSubCounty() != null
                        && !e.getRegion().isEmpty() && !e.getSubRegion().isEmpty() && !e.getLocalGovernment().isEmpty()
                        && !e.getCounty().isEmpty() && !e.getSubCounty().isEmpty())
                .filter(sharedService.distinctByKey(e -> e.getRegion() + ":" + e.getSubRegion() + ":"
                        + e.getLocalGovernment() + ":" + e.getCounty() + ":" + e.getSubCounty()))
                .filter(excel -> {
                    String key = (excel.getRegion() + "_" + excel.getSubRegion() + "_" + excel.getLocalGovernment()
                            + "_" + excel.getCounty()).toLowerCase();
                    return countyMap.containsKey(key);
                })
                .filter(excel -> !subCountyRepository
                        .existsByDetails(excel.getSubCounty(), excel.getCounty(), excel.getLocalGovernment(),
                                excel.getSubRegion(), excel.getRegion()))
                .map(excel -> {
                    String key = (excel.getRegion() + "_" + excel.getSubRegion() + "_" + excel.getLocalGovernment()
                            + "_" + excel.getCounty()).toLowerCase();
                    County county = countyMap.get(key);
                    SubCounty subCounty = SubCounty.builder()
                            .name(excel.getSubCounty())
                            .code(sharedService.generateUniqueCode(AdministrativeAreaType.SUBCOUNTY,
                                    code -> subCountyRepository.existsByCodeIgnoreCase(code)))
                            .county(county)
                            .build();
                    return subCounty;
                })
                .toList();

        if (!uniqueSubCounties.isEmpty()) {
            subCountyRepository.saveAll(uniqueSubCounties);
        }

        // search for existing sub counties, join county
        Set<String> involvedCounties = dtoList.stream()
                .filter(e -> e.getCounty() != null)
                .map(e -> e.getCounty().toLowerCase())
                .collect(Collectors.toSet());

        Specification<SubCounty> spec = (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType()) {
                Fetch<SubCounty, County> countyFetch = root.fetch("county", JoinType.LEFT);
                Fetch<County, LocalGovernment> localGovernmentFetch = countyFetch.fetch("localGovernment",
                        JoinType.LEFT);
                Fetch<LocalGovernment, SubRegion> subRegionFetch = localGovernmentFetch.fetch("subRegion",
                        JoinType.LEFT);
                subRegionFetch.fetch("region", JoinType.LEFT);
            }
            return cb.lower(root.get("county").get("name")).in(involvedCounties);
        };
        spec = spec.and(new GenericSpecification<>(new SearchCriteria("archived", "false", MatchType.EQUALS)));

        int page = 0;
        int size = 1000;
        List<SubCounty> existingSubCounties = new java.util.ArrayList<>();
        Page<SubCounty> subCountyPage = subCountyRepository.findAll(spec, PageRequest.of(page, size));

        while (subCountyPage.hasNext()) {
            existingSubCounties.addAll(subCountyPage.getContent());
            subCountyPage = subCountyRepository.findAll(spec, PageRequest.of(++page, size));
        }
        // add remaining elements
        if (!subCountyPage.getContent().isEmpty()) {
            existingSubCounties.addAll(subCountyPage.getContent());
        }

        return existingSubCounties;
    }

    @Override
    public Optional<SubCounty> findByCode(String code) {
        return subCountyRepository.findByCodeIgnoreCase(code);
    }

    @Override

    public List<SubCounty> findByNames(List<String> names) {
        return subCountyRepository.findByNameIgnoreCaseIn(names);
    }

    @Override
    @Transactional
    public List<SubCounty> saveAll(List<SubCounty> subCounties) {
        return subCountyRepository.saveAll(subCounties);
    }

    private final ParishRepository parishRepository;

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.SUB_COUNTIES,
            CacheValueKeyConfig.SUB_COUNTIES_FILTERED }, allEntries = true)
    public ResponseDTO<String> delete(String code) {
        if (parishRepository.existsBySubCounty_Code(code)) {
            throw new InvalidException("Sub County with code " + code + " cannot be deleted because it has parishes");
        }
        SubCounty subCounty = subCountyRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("Sub County with code " + code + " not found"));
        subCountyRepository.delete(subCounty);
        return new ResponseDTO<>("SUCCESS", "Sub County deleted successfully");
    }

    private String generateCode() {
        return sharedService.generateCode(AdministrativeAreaType.SUBCOUNTY);
    }
}
