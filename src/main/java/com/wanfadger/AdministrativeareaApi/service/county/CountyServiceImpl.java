package com.wanfadger.AdministrativeareaApi.service.county;

import com.wanfadger.AdministrativeareaApi.areaexceptions.AlreadyExistsException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.MissingDataException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.NotFoundException;
import com.wanfadger.AdministrativeareaApi.dto.ExcelJsonDTO;
import com.wanfadger.AdministrativeareaApi.dto.CountyDTO;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;

import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.entity.County;
import com.wanfadger.AdministrativeareaApi.entity.LocalGovernment;
import com.wanfadger.AdministrativeareaApi.entity.Region;
import com.wanfadger.AdministrativeareaApi.entity.SubRegion;
import com.wanfadger.AdministrativeareaApi.repository.CountyRepository;
import com.wanfadger.AdministrativeareaApi.repository.LocalGovernmentRepository;
import com.wanfadger.AdministrativeareaApi.repository.SubCountyRepository;
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
public class CountyServiceImpl implements CountyService {

    private final CountyRepository countyRepository;
    private final LocalGovernmentRepository localGovernmentRepository;
    private final SharedService sharedService;
    private final CountyMapperService countyMapperService;

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.COUNTIES }, allEntries = true)
    public ResponseDTO<String> create(NewAdministrativeAreaDTO dto) {
        if (dto.getParentCode() == null || dto.getParentCode().isEmpty()) {
            throw new MissingDataException("Missing PartOfCode(local government) for the county");
        }

        LocalGovernment localGovernment = localGovernmentRepository.findByCodeIgnoreCase(dto.getParentCode())
                .orElseThrow(() -> new InvalidException("Invalid PartOfCode: " + dto.getParentCode()));

        if (countyRepository.existsByNameIgnoreCaseAndLocalGovernment_Code(dto.getName(), dto.getParentCode())) {
            throw new AlreadyExistsException("County " + dto.getName() + " Already Exists in the local government");
        }

        County county = countyMapperService.toCounty(dto, localGovernment);

        countyRepository.save(county);

        return new ResponseDTO<>(county.getCode(), "successfully created a county");
    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.COUNTIES }, allEntries = true)
    public ResponseDTO<String> createAll(List<NewAdministrativeAreaDTO> dtos) {
        if (dtos.parallelStream().anyMatch(dto -> dto.getParentCode() == null || dto.getParentCode().isEmpty())) {
            throw new MissingDataException("Found Administrative Area without PartOfCoce");
        }

        List<String> localGovernmentCodes = dtos.stream()
                .map(NewAdministrativeAreaDTO::getParentCode)
                .collect(Collectors.toList());

        Map<String, LocalGovernment> localGovernmentMap = localGovernmentRepository
                .findByCodeIgnoreCaseIn(localGovernmentCodes).stream()
                .collect(Collectors.toMap(LocalGovernment::getCode, lg -> lg, (existing, replacement) -> existing));

        List<County> counties = dtos.parallelStream()
                .filter(dto -> !countyRepository
                        .existsByNameIgnoreCaseAndLocalGovernment_Code(dto.getName(), dto.getParentCode()))
                .filter(dto -> localGovernmentMap.containsKey(dto.getParentCode()))
                .map(dto -> {
                    LocalGovernment localGovernment = localGovernmentMap.get(dto.getParentCode());
                    return countyMapperService.toCounty(dto, localGovernment);
                })
                .toList();

        countyRepository.saveAll(counties);

        return new ResponseDTO<>("success",
                "successfully added " + counties.size() + " administrative areas");
    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.COUNTIES, CacheValueKeyConfig.COUNTIES_FILTERED }, allEntries = true)
    public ResponseDTO<String> update(UpdateAdministrativeAreaDTO dto) {
        if (dto.getParentCode() == null || dto.getParentCode().isEmpty()) {
            throw new MissingDataException("Missing parent code");
        }

        LocalGovernment localGovernment = localGovernmentRepository.findByCodeIgnoreCase(dto.getParentCode())
                .orElseThrow(() -> new NotFoundException(
                        "Local Government with code " + dto.getParentCode() + " not found"));
        County county = countyRepository.findByCodeIgnoreCaseAndLocalGovernment_Code(dto.getCode(), dto.getParentCode())
                .orElseThrow(() -> new NotFoundException("County with code " + dto.getCode()
                        + " not found in local government " + localGovernment.getName()));

        if (dto.getName() != null && !dto.getName().isEmpty() && !county.getName().equalsIgnoreCase(dto.getName())) {
            if (countyRepository.existsByNameIgnoreCaseAndLocalGovernment_Code(dto.getName(), dto.getParentCode())) {
                throw new AlreadyExistsException(
                        "County " + dto.getName() + " Already Exists in the " + localGovernment.getName()
                                + " local government");
            }
            county.setName(dto.getName());
        }

        if (dto.getLatitude() != null && !dto.getLatitude().isEmpty()
                && !county.getLatitude().toString().equalsIgnoreCase(dto.getLatitude())) {
            county.setLatitude(Double.valueOf(dto.getLatitude()));
        }

        if (dto.getLongitude() != null && !dto.getLongitude().isEmpty()
                && !county.getLongitude().toString().equalsIgnoreCase(dto.getLongitude())) {
            county.setLongitude(Double.valueOf(dto.getLongitude()));
        }

        if (dto.getDescription() != null && !dto.getDescription().isEmpty()
                && !county.getDescription().equalsIgnoreCase(dto.getDescription())) {
            county.setDescription(dto.getDescription());
        }

        countyRepository.save(county);

        return new ResponseDTO<>(county.getCode(), "successfully updated county");
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.COUNTIES, key = "#code")
    public ResponseDTO<CountyDTO> getByCode(String code) {
        County county = countyRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("County not found"));
        return new ResponseDTO<>(countyMapperService.toDTO(county));
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.COUNTIES, key = "#code+'_details'")
    public ResponseDTO<CountyDTO> findDetailsByCode(String code) {
        County county = countyRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("County not found"));
        return new ResponseDTO<>(countyMapperService.toDTO(county));
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.COUNTIES_FILTERED, keyGenerator = "sortedMapKeyGenerator", unless = "#result.totalElements == 0")
    @Transactional(readOnly = true)
    public PaginatedResponseDTO<CountyDTO> filter(Map<String, String> queryMap) {
        // 1. Extract Pagination & Sorting
        int page = Optional.ofNullable(queryMap.get("page")).map(Integer::parseInt).orElse(1);
        int size = Optional.ofNullable(queryMap.get("size")).map(Integer::parseInt).orElse(10);
        String sortBy = Optional.ofNullable(queryMap.get("sortBy")).orElse("name");
        String sortDirection = Optional.ofNullable(queryMap.get("sortDirection")).orElse("ASC");

        page = page <= 0 ? 0 : page - 1;

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(sortDirection.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy));

        // 2. Build Specification, including generic filtering
        Specification<County> spec = new GenericSpecification<>(
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
                    new SearchCriteria("localGovernment.code", queryMap.get("selected"), MatchType.EQUALS)));
        }

        // 3. Execute Search
        Page<County> resultPage = countyRepository.findAll(spec, pageable);

        // 4. Map to DTOs
        List<CountyDTO> data = resultPage.getContent().stream()
                .map(countyMapperService::toDTO)
                .toList();

        // 5. Build Paginated Response
        PaginatedResponseDTO<CountyDTO> response = new PaginatedResponseDTO<>();
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
    @Cacheable(value = CacheValueKeyConfig.COUNTIES, key = "#queryMap.toString()", unless = "#result.totalElements == 0")
    public PaginatedResponseDTO<CountyDTO> search(Map<String, String> queryMap) {
        // 1. Extract Pagination & Sorting
        int page = Optional.ofNullable(queryMap.get("page")).map(Integer::parseInt).orElse(1);
        int size = Optional.ofNullable(queryMap.get("size")).map(Integer::parseInt).orElse(10);
        String sortBy = Optional.ofNullable(queryMap.get("sortBy")).orElse("name");
        String sortDirection = Optional.ofNullable(queryMap.get("sortDirection")).orElse("ASC");

        page = page <= 0 ? 0 : page - 1;

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(sortDirection.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy));

        // 2. Build Specification
        Specification<County> spec = (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType()) {
                Fetch<County, LocalGovernment> localGovernmentFetch = root.fetch("localGovernment", JoinType.LEFT);
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
        Page<County> resultPage = countyRepository.findAll(spec, pageable);

        // 4. Map to DTOs
        List<CountyDTO> data = resultPage.getContent().stream()
                .map(countyMapperService::toDetailDTO) // Changed to toDetailDTO
                .toList();

        // 5. Build Paginated Response
        PaginatedResponseDTO<CountyDTO> response = new PaginatedResponseDTO<>();
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
    @CacheEvict(value = { CacheValueKeyConfig.COUNTIES }, allEntries = true)
    public List<County> upload(List<ExcelJsonDTO> dtoList, Map<String, LocalGovernment> localGovernmentMap) {
        // filter unique counties in each local government
        List<County> uniqueCounties = dtoList.stream()
                .filter(e -> e.getRegion() != null && e.getSubRegion() != null && e.getLocalGovernment() != null
                        && e.getCounty() != null
                        && !e.getRegion().isEmpty() && !e.getSubRegion().isEmpty() && !e.getLocalGovernment().isEmpty()
                        && !e.getCounty().isEmpty())
                .filter(sharedService.distinctByKey(e -> e.getRegion() + ":" + e.getSubRegion() + ":"
                        + e.getLocalGovernment() + ":" + e.getCounty()))
                .filter(excel -> localGovernmentMap.containsKey((excel.getRegion() + "_" + excel.getSubRegion() + "_"
                        + excel.getLocalGovernment()).toLowerCase()))
                .filter(excel -> !countyRepository.existsByDetails(
                        excel.getCounty(), excel.getLocalGovernment(), excel.getSubRegion(), excel.getRegion()))
                .map(excel -> {
                    String key = (excel.getRegion() + "_" + excel.getSubRegion() + "_" + excel.getLocalGovernment())
                            .toLowerCase();
                    LocalGovernment localGovernment = localGovernmentMap.get(key);
                    County county = County.builder()
                            .name(excel.getCounty())
                            .code(sharedService.generateUniqueCode(AdministrativeAreaType.COUNTY,
                                    code -> countyRepository.existsByCodeIgnoreCase(code)))
                            .localGovernment(localGovernment)
                            .build();
                    return county;
                })
                .toList();

        if (!uniqueCounties.isEmpty()) {
            countyRepository.saveAll(uniqueCounties);
        }

        // search for existing counties, join local government
        Set<String> involvedLGs = dtoList.stream()
                .filter(e -> e.getLocalGovernment() != null)
                .map(e -> e.getLocalGovernment().toLowerCase())
                .collect(Collectors.toSet());

        Specification<County> spec = (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType()) {
                Fetch<County, LocalGovernment> localGovernmentFetch = root.fetch("localGovernment", JoinType.LEFT);
                Fetch<LocalGovernment, SubRegion> subRegionFetch = localGovernmentFetch.fetch("subRegion",
                        JoinType.LEFT);
                subRegionFetch.fetch("region", JoinType.LEFT);
            }
            return cb.lower(root.get("localGovernment").get("name")).in(involvedLGs);
        };
        spec = spec.and(new GenericSpecification<>(new SearchCriteria("archived", "false", MatchType.EQUALS)));

        int page = 0;
        int size = 1000; // Increased batch size for efficiency
        List<County> existingCounties = new java.util.ArrayList<>();
        Page<County> countyPage = countyRepository.findAll(spec, PageRequest.of(page, size));

        while (countyPage.hasNext()) {
            existingCounties.addAll(countyPage.getContent());
            countyPage = countyRepository.findAll(spec, PageRequest.of(++page, size));
        }
        // add remaining elements
        if (!countyPage.getContent().isEmpty()) {
            existingCounties.addAll(countyPage.getContent());
        }

        return existingCounties;
    }

    @Override
    public Optional<County> findByCode(String code) {
        return countyRepository.findByCodeIgnoreCase(code);
    }

    @Override
    public List<County> findByNames(List<String> names) {
        return countyRepository.findByNameIgnoreCaseIn(names);
    }

    @Override
    @Transactional
    public List<County> saveAll(List<County> counties) {
        return countyRepository.saveAll(counties);
    }

    private final SubCountyRepository subCountyRepository;

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.COUNTIES, CacheValueKeyConfig.COUNTIES_FILTERED }, allEntries = true)
    public ResponseDTO<String> delete(String code) {
        if (subCountyRepository.existsByCounty_Code(code)) {
            throw new InvalidException("County with code " + code + " cannot be deleted because it has sub counties");
        }
        County county = countyRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("County with code " + code + " not found"));
        countyRepository.delete(county);
        return new ResponseDTO<>("SUCCESS", "County deleted successfully");
    }

}
