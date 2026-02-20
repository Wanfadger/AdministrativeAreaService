package com.wanfadger.AdministrativeareaApi.service.parish;

import com.wanfadger.AdministrativeareaApi.areaexceptions.AlreadyExistsException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.MissingDataException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.NotFoundException;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.dto.ExcelJsonDTO;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.ParishDTO;

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
    private final ParishMapperService parishMapperService;

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.PARISHES }, allEntries = true)
    public ResponseDTO<String> create(NewAdministrativeAreaDTO dto) {
        if (dto.getParentCode() == null || dto.getParentCode().isEmpty()) {
            throw new MissingDataException("Missing PartOfCode(sub county) for the parish");
        }

        SubCounty subCounty = subCountyRepository.findByCodeIgnoreCase(dto.getParentCode())
                .orElseThrow(() -> new InvalidException("Invalid PartOfCode: " + dto.getParentCode()));

        if (parishRepository.existsByNameIgnoreCaseAndSubCounty_Code(dto.getName(), dto.getParentCode())) {
            throw new AlreadyExistsException("Parish " + dto.getName() + " Already Exists in the sub county");
        }

        Parish parish = parishMapperService.toParish(dto, subCounty);

        parishRepository.save(parish);

        return new ResponseDTO<>(parish.getCode(), "successfully created a parish");
    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.PARISHES }, allEntries = true)
    public ResponseDTO<String> createAll(List<NewAdministrativeAreaDTO> dtos) {
        if (dtos.parallelStream().anyMatch(dto -> dto.getParentCode() == null || dto.getParentCode().isEmpty())) {
            throw new MissingDataException("Found Administrative Area without PartOfCoce");
        }

        List<String> subCountyCodes = dtos.stream()
                .map(NewAdministrativeAreaDTO::getParentCode)
                .collect(Collectors.toList());

        Map<String, SubCounty> subCountyMap = subCountyRepository.findByCodeIgnoreCaseIn(subCountyCodes).stream()
                .collect(Collectors.toMap(SubCounty::getCode, sc -> sc, (existing, replacement) -> existing));

        List<Parish> parishes = dtos.parallelStream()
                .filter(dto -> !parishRepository
                        .existsByNameIgnoreCaseAndSubCounty_Code(dto.getName(), dto.getParentCode()))
                .filter(dto -> subCountyMap.containsKey(dto.getParentCode()))
                .map(dto -> {
                    SubCounty subCounty = subCountyMap.get(dto.getParentCode());
                    return parishMapperService.toParish(dto, subCounty);
                })
                .toList();

        parishRepository.saveAll(parishes);

        return new ResponseDTO<>("success",
                "successfully added " + parishes.size() + " administrative areas");
    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.PARISHES, CacheValueKeyConfig.PARISHES_FILTERED }, allEntries = true)
    public ResponseDTO<String> update(UpdateAdministrativeAreaDTO dto) {
        if (dto.getParentCode() == null || dto.getParentCode().isEmpty()) {
            throw new MissingDataException("Missing parent code");
        }

        SubCounty subCounty = subCountyRepository.findByCodeIgnoreCase(dto.getParentCode())
                .orElseThrow(() -> new NotFoundException("Sub county with code " + dto.getParentCode() + " not found"));
        Parish parish = parishRepository.findByCodeIgnoreCaseAndSubCounty_Code(dto.getCode(), dto.getParentCode())
                .orElseThrow(() -> new NotFoundException(
                        "Parish with code " + dto.getCode() + " not found in sub county " + subCounty.getName()));

        if (dto.getName() != null && !dto.getName().isEmpty() && !parish.getName().equalsIgnoreCase(dto.getName())) {
            if (parishRepository.existsByNameIgnoreCaseAndSubCounty_Code(dto.getName(), dto.getParentCode())) {
                throw new AlreadyExistsException(
                        "Parish " + dto.getName() + " Already Exists in the " + subCounty.getName() + " sub county");
            }
            parish.setName(dto.getName());
        }

        if (dto.getLatitude() != null && !dto.getLatitude().isEmpty()
                && !parish.getLatitude().toString().equalsIgnoreCase(dto.getLatitude())) {
            parish.setLatitude(Double.valueOf(dto.getLatitude()));
        }

        if (dto.getLongitude() != null && !dto.getLongitude().isEmpty()
                && !parish.getLongitude().toString().equalsIgnoreCase(dto.getLongitude())) {
            parish.setLongitude(Double.valueOf(dto.getLongitude()));
        }

        if (dto.getDescription() != null && !dto.getDescription().isEmpty()
                && !parish.getDescription().equalsIgnoreCase(dto.getDescription())) {
            parish.setDescription(dto.getDescription());
        }

        parishRepository.save(parish);

        return new ResponseDTO<>(parish.getCode(), "successfully updated parish");
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.PARISHES, key = "#code")
    public ResponseDTO<ParishDTO> getByCode(String code) {
        Parish parish = parishRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("Parish not found"));
        return new ResponseDTO<>(parishMapperService.toDTO(parish));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheValueKeyConfig.PARISHES, key = "#code+'_details'")
    public ResponseDTO<ParishDTO> getDetailsByCode(String code) {
        Parish parish = parishRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("Parish not found"));
        return new ResponseDTO<>(parishMapperService.toDetailDTO(parish));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheValueKeyConfig.PARISHES_FILTERED, keyGenerator = "sortedMapKeyGenerator", unless = "#result.totalElements == 0")
    public PaginatedResponseDTO<ParishDTO> filter(Map<String, String> queryMap) {
        // 1. Extract Pagination & Sorting
        int page = Optional.ofNullable(queryMap.get("page")).map(Integer::parseInt).orElse(1);
        int size = Optional.ofNullable(queryMap.get("size")).map(Integer::parseInt).orElse(10);
        String sortBy = Optional.ofNullable(queryMap.get("sortBy")).orElse("name");
        String sortDirection = Optional.ofNullable(queryMap.get("sortDirection")).orElse("ASC");

        page = page <= 0 ? 0 : page - 1;

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(sortDirection.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy));

        // 2. Build Specification, including generic filtering
        Specification<Parish> spec = new GenericSpecification<>(
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
                    new SearchCriteria("subCounty.code", queryMap.get("selected"), MatchType.EQUALS)));
        }

        // 3. Execute Search
        Page<Parish> resultPage = parishRepository.findAll(spec, pageable);

        // 4. Map to DTOs
        List<ParishDTO> data = resultPage.getContent().stream()
                .map(parishMapperService::toDTO)
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
    @Transactional(readOnly = true)
    @Cacheable(value = CacheValueKeyConfig.PARISHES, keyGenerator = "sortedMapKeyGenerator", unless = "#result.totalElements == 0")
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
                .map(parishMapperService::toDTO)
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
    public List<Parish> upload(List<ExcelJsonDTO> dtoList, Map<String, SubCounty> subCountyMap) {
        // filter unique parishes in each sub county
        List<Parish> uniqueParishes = dtoList.stream()
                .filter(e -> e.getRegion() != null && e.getSubRegion() != null && e.getLocalGovernment() != null
                        && e.getCounty() != null && e.getSubCounty() != null && e.getParish() != null
                        && !e.getRegion().isEmpty() && !e.getSubRegion().isEmpty() && !e.getLocalGovernment().isEmpty()
                        && !e.getCounty().isEmpty() && !e.getSubCounty().isEmpty() && !e.getParish().isEmpty())
                .filter(sharedService.distinctByKey(e -> e.getRegion() + ":" + e.getSubRegion() + ":"
                        + e.getLocalGovernment() + ":" + e.getCounty() + ":" + e.getSubCounty() + ":" + e.getParish()))
                .filter(excel -> {
                    String key = (excel.getRegion() + "_" + excel.getSubRegion() + "_" + excel.getLocalGovernment()
                            + "_" + excel.getCounty() + "_" + excel.getSubCounty()).toLowerCase();
                    return subCountyMap.containsKey(key);
                })
                .filter(excel -> !parishRepository.existsByDetails(excel.getParish(),
                        excel.getSubCounty(), excel.getCounty(), excel.getLocalGovernment(), excel.getSubRegion(),
                        excel.getRegion()))
                .map(excel -> {
                    String key = (excel.getRegion() + "_" + excel.getSubRegion() + "_" + excel.getLocalGovernment()
                            + "_" + excel.getCounty() + "_" + excel.getSubCounty()).toLowerCase();
                    SubCounty subCounty = subCountyMap.get(key);
                    Parish parish = Parish.builder()
                            .name(excel.getParish())
                            .code(generateCode())
                            .subCounty(subCounty)
                            .build();
                    return parish;
                })
                .toList();

        if (!uniqueParishes.isEmpty()) {
            parishRepository.saveAll(uniqueParishes);
        }

        // search for existing parishes, join sub county
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
        spec = spec.and(new GenericSpecification<>(new SearchCriteria("archived", "false", MatchType.EQUALS)));

        int page = 0;
        int size = 100;
        List<Parish> existingParishes = new java.util.ArrayList<>();
        Page<Parish> parishPage = parishRepository.findAll(spec, PageRequest.of(page, size));

        while (parishPage.hasNext()) {
            existingParishes.addAll(parishPage.getContent());
            parishPage = parishRepository.findAll(spec, PageRequest.of(++page, size));
        }
        // add remaining elements
        if (!parishPage.getContent().isEmpty()) {
            existingParishes.addAll(parishPage.getContent());
        }

        return existingParishes;
    }

    @Override
    public List<Parish> findByNames(List<String> names) {
        return parishRepository.findByNameIgnoreCaseIn(names);
    }

    @Override
    @Transactional
    public List<Parish> saveAll(List<Parish> parishes) {
        return parishRepository.saveAll(parishes);
    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.PARISHES, CacheValueKeyConfig.PARISHES_FILTERED }, allEntries = true)
    public ResponseDTO<String> delete(String code) {
        Parish parish = parishRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("Parish with code " + code + " not found"));
        parishRepository.delete(parish);
        return new ResponseDTO<>("SUCCESS", "Parish deleted successfully");
    }

    private String generateCode() {
        return sharedService.generateCode(AdministrativeAreaType.PARISH);
    }
}
