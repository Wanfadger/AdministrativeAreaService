package com.wanfadger.AdministrativeareaApi.service.localgovernment;

import com.wanfadger.AdministrativeareaApi.areaexceptions.AlreadyExistsException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.MissingDataException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.NotFoundException;
import com.wanfadger.AdministrativeareaApi.dto.ExcelJsonDTO;
import com.wanfadger.AdministrativeareaApi.dto.LocalGovernmentDTO;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;

import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.entity.LocalGovernment;
import com.wanfadger.AdministrativeareaApi.entity.Region;
import com.wanfadger.AdministrativeareaApi.entity.SubRegion;
import com.wanfadger.AdministrativeareaApi.repository.LocalGovernmentRepository;
import com.wanfadger.AdministrativeareaApi.repository.SubRegionRepository;
import com.wanfadger.AdministrativeareaApi.repository.CountyRepository;
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
public class LocalGovernmentServiceImpl implements LocalGovernmentService {

    private final LocalGovernmentRepository localGovernmentRepository;
    private final SubRegionRepository subRegionRepository;
    private final SharedService sharedService;
    private final LocalGovernmentMapperService localGovernmentMapperService;

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.LOCAL_GOVERNMENTS,
            CacheValueKeyConfig.LOCAL_GOVERNMENTS_FILTERED }, allEntries = true)
    public ResponseDTO<String> create(NewAdministrativeAreaDTO dto) {
        if (dto.getParentCode() == null || dto.getParentCode().isEmpty()) {
            throw new MissingDataException("Missing PartOfCode(sub region) for the local government");
        }

        SubRegion subRegion = subRegionRepository.findByCodeIgnoreCase(dto.getParentCode())
                .orElseThrow(() -> new InvalidException("Invalid PartOfCode: " + dto.getParentCode()));

        if (localGovernmentRepository.existsByNameIgnoreCaseAndSubRegion_Code(dto.getName(), dto.getParentCode())) {
            throw new AlreadyExistsException("Local Government " + dto.getName() + " Already Exists in the sub region");
        }

        LocalGovernment localGovernment = localGovernmentMapperService.toLocalGovernment(dto, subRegion);

        localGovernmentRepository.save(localGovernment);

        return new ResponseDTO<>(localGovernment.getCode(),
                "successfully created a local government");
    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.LOCAL_GOVERNMENTS,
            CacheValueKeyConfig.LOCAL_GOVERNMENTS_FILTERED }, allEntries = true)
    public ResponseDTO<String> createAll(List<NewAdministrativeAreaDTO> dtos) {
        if (dtos.parallelStream().anyMatch(dto -> dto.getParentCode() == null || dto.getParentCode().isEmpty())) {
            throw new MissingDataException("Found Administrative Area without PartOfCoce");
        }

        List<String> subRegionCodes = dtos.stream()
                .map(NewAdministrativeAreaDTO::getParentCode)
                .collect(Collectors.toList());

        Map<String, SubRegion> subRegionMap = subRegionRepository.findByCodeIgnoreCaseIn(subRegionCodes).stream()
                .collect(Collectors.toMap(SubRegion::getCode, subRegion -> subRegion,
                        (existing, replacement) -> existing));

        List<LocalGovernment> localGovernments = dtos.parallelStream()
                .filter(dto -> !localGovernmentRepository
                        .existsByNameIgnoreCaseAndSubRegion_Code(dto.getName(), dto.getParentCode()))
                .filter(dto -> subRegionMap.containsKey(dto.getParentCode()))
                .map(dto -> {
                    SubRegion subRegion = subRegionMap.get(dto.getParentCode());
                    return localGovernmentMapperService.toLocalGovernment(dto, subRegion);
                })
                .toList();

        localGovernmentRepository.saveAll(localGovernments);

        return new ResponseDTO<>("success",
                "successfully added " + localGovernments.size() + " administrative areas");
    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.LOCAL_GOVERNMENTS,
            CacheValueKeyConfig.LOCAL_GOVERNMENTS_FILTERED }, allEntries = true)
    public ResponseDTO<String> update(UpdateAdministrativeAreaDTO dto) {
        if (dto.getParentCode() == null || dto.getParentCode().isEmpty()) {
            throw new MissingDataException("Missing parent code");
        }

        SubRegion subRegion = subRegionRepository.findByCodeIgnoreCase(dto.getParentCode())
                .orElseThrow(() -> new NotFoundException("Sub region with code " + dto.getParentCode() + " not found"));

        LocalGovernment localGovernment = localGovernmentRepository
                .findByCodeIgnoreCaseAndSubRegion_Code(dto.getCode(), dto.getParentCode())
                .orElseThrow(() -> new NotFoundException("Local government with code " + dto.getCode()
                        + " not found in sub region " + subRegion.getName()));

        if (dto.getName() != null && !dto.getName().isEmpty()
                && !localGovernment.getName().equalsIgnoreCase(dto.getName())) {
            if (localGovernmentRepository.existsByNameIgnoreCaseAndSubRegion_Code(dto.getName(), dto.getParentCode())) {
                throw new AlreadyExistsException("Local Government " + dto.getName() + " Already Exists in the "
                        + subRegion.getName() + " sub region");
            }
            localGovernment.setName(dto.getName());
        }

        if (dto.getLatitude() != null && !dto.getLatitude().isEmpty()
                && !localGovernment.getLatitude().toString().equalsIgnoreCase(dto.getLatitude())) {
            localGovernment.setLatitude(Double.valueOf(dto.getLatitude()));
        }

        if (dto.getLongitude() != null && !dto.getLongitude().isEmpty()
                && !localGovernment.getLongitude().toString().equalsIgnoreCase(dto.getLongitude())) {
            localGovernment.setLongitude(Double.valueOf(dto.getLongitude()));
        }

        if (dto.getDescription() != null && !dto.getDescription().isEmpty()
                && !localGovernment.getDescription().equalsIgnoreCase(dto.getDescription())) {
            localGovernment.setDescription(dto.getDescription());
        }

        localGovernmentRepository.save(localGovernment);

        return new ResponseDTO<>(localGovernment.getCode(), "successfully updated local government");
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.LOCAL_GOVERNMENTS, key = "#code")
    public ResponseDTO<LocalGovernmentDTO> getByCode(String code) {
        LocalGovernment localGovernment = localGovernmentRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("LocalGovernment not found"));
        return new ResponseDTO<>(localGovernmentMapperService.toDTO(localGovernment));
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.LOCAL_GOVERNMENTS, key = "#code+'_details'")
    public ResponseDTO<LocalGovernmentDTO> findDetailsByCode(String code) {
        LocalGovernment localGovernment = localGovernmentRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("LocalGovernment not found"));
        return new ResponseDTO<>(localGovernmentMapperService.toDTO(localGovernment));
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.LOCAL_GOVERNMENTS_FILTERED, keyGenerator = "sortedMapKeyGenerator", unless = "#result.totalElements == 0")
    @Transactional(readOnly = true)
    public PaginatedResponseDTO<LocalGovernmentDTO> filter(Map<String, String> queryMap) {
        // 1. Extract Pagination & Sorting
        int page = Optional.ofNullable(queryMap.get("page")).map(Integer::parseInt).orElse(1);
        int size = Optional.ofNullable(queryMap.get("size")).map(Integer::parseInt).orElse(10);
        String sortBy = Optional.ofNullable(queryMap.get("sortBy")).orElse("name");
        String sortDirection = Optional.ofNullable(queryMap.get("sortDirection")).orElse("ASC");

        page = page <= 0 ? 0 : page - 1;

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(sortDirection.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy));

        // 2. Build Specification, including generic filtering
        Specification<LocalGovernment> spec = new GenericSpecification<>(
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
                    new SearchCriteria("subRegion.code", queryMap.get("selected"), MatchType.EQUALS)));
        }

        // 3. Execute Search
        Page<LocalGovernment> resultPage = localGovernmentRepository.findAll(spec, pageable);

        // 4. Map to DTOs
        List<LocalGovernmentDTO> data = resultPage.getContent().stream()
                .map(localGovernmentMapperService::toDTO)
                .toList();

        // 5. Build Paginated Response
        PaginatedResponseDTO<LocalGovernmentDTO> response = new PaginatedResponseDTO<>();
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
    @Cacheable(value = CacheValueKeyConfig.LOCAL_GOVERNMENTS, key = "#queryMap.toString()", unless = "#result.totalElements == 0")
    public PaginatedResponseDTO<LocalGovernmentDTO> search(Map<String, String> queryMap) {
        // 1. Extract Pagination & Sorting
        int page = Optional.ofNullable(queryMap.get("page")).map(Integer::parseInt).orElse(1);
        int size = Optional.ofNullable(queryMap.get("size")).map(Integer::parseInt).orElse(10);
        String sortBy = Optional.ofNullable(queryMap.get("sortBy")).orElse("name");
        String sortDirection = Optional.ofNullable(queryMap.get("sortDirection")).orElse("ASC");

        page = page <= 0 ? 0 : page - 1;

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(sortDirection.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy));

        // 2. Build Specification
        Specification<LocalGovernment> spec = (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType()) {
                Fetch<LocalGovernment, SubRegion> subRegionFetch = root.fetch("subRegion", JoinType.LEFT);
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
        Page<LocalGovernment> resultPage = localGovernmentRepository.findAll(spec, pageable);

        // 4. Map to DTOs
        List<LocalGovernmentDTO> data = resultPage.getContent().stream()
                .map(localGovernmentMapperService::toDetailDTO)
                .toList();

        // 5. Build Paginated Response
        PaginatedResponseDTO<LocalGovernmentDTO> response = new PaginatedResponseDTO<>();
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
    @CacheEvict(value = { CacheValueKeyConfig.LOCAL_GOVERNMENTS }, allEntries = true)
    public List<LocalGovernment> upload(List<ExcelJsonDTO> dtoList, Map<String, SubRegion> subRegionMap) {
        // filter unique local governments in each sub region
        List<LocalGovernment> uniqueLGs = dtoList.stream()
                .filter(e -> e.getRegion() != null && e.getSubRegion() != null && e.getLocalGovernment() != null
                        && !e.getRegion().isEmpty() && !e.getSubRegion().isEmpty() && !e.getLocalGovernment().isEmpty())
                .filter(sharedService
                        .distinctByKey(e -> e.getRegion() + ":" + e.getSubRegion() + ":" + e.getLocalGovernment()))
                .filter(excel -> subRegionMap
                        .containsKey((excel.getRegion() + "_" + excel.getSubRegion()).toLowerCase()))
                .filter(excel -> !localGovernmentRepository
                        .existsByDetails(
                                excel.getLocalGovernment(), excel.getSubRegion(), excel.getRegion()))
                .map(excel -> {
                    String key = (excel.getRegion() + "_" + excel.getSubRegion()).toLowerCase();
                    SubRegion subRegion = subRegionMap.get(key);
                    LocalGovernment localGovernment = LocalGovernment.builder()
                            .name(excel.getLocalGovernment())
                            .code(sharedService.generateUniqueCode(AdministrativeAreaType.LOCALGOVERNMENT, code -> localGovernmentRepository.existsByCodeIgnoreCase(code)))
                            .subRegion(subRegion)
                            .build();
                    return localGovernment;
                })
                .toList();

        if (!uniqueLGs.isEmpty()) {
            localGovernmentRepository.saveAll(uniqueLGs);
        }

        // search for existing local governments, join sub region
        Set<String> involvedSubRegions = dtoList.stream()
                .filter(e -> e.getSubRegion() != null)
                .map(e -> e.getSubRegion().toLowerCase())
                .collect(Collectors.toSet());

        Specification<LocalGovernment> spec = (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType()) {
                Fetch<LocalGovernment, SubRegion> subRegionFetch = root.fetch("subRegion", JoinType.LEFT);
                subRegionFetch.fetch("region", JoinType.LEFT);
            }
            return cb.lower(root.get("subRegion").get("name")).in(involvedSubRegions);
        };
        spec = spec.and(new GenericSpecification<>(new SearchCriteria("archived", "false", MatchType.EQUALS)));

        int page = 0;
        int size = 1000;
        List<LocalGovernment> existingLGs = new java.util.ArrayList<>();
        Page<LocalGovernment> lgPage = localGovernmentRepository.findAll(spec, PageRequest.of(page, size));

        while (lgPage.hasNext()) {
            existingLGs.addAll(lgPage.getContent());
            lgPage = localGovernmentRepository.findAll(spec, PageRequest.of(++page, size));
        }
        // add remaining elements
        if (!lgPage.getContent().isEmpty()) {
            existingLGs.addAll(lgPage.getContent());
        }

        return existingLGs;
    }

    @Override
    public Optional<LocalGovernment> findByCode(String code) {
        return localGovernmentRepository.findByCodeIgnoreCase(code);
    }

    @Override
    public List<LocalGovernment> findByNames(List<String> names) {
        return localGovernmentRepository.findByNameIgnoreCaseIn(names);
    }

    @Override
    @Transactional
    public List<LocalGovernment> saveAll(List<LocalGovernment> localGovernments) {
        return localGovernmentRepository.saveAll(localGovernments);
    }

    private final CountyRepository countyRepository;

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.LOCAL_GOVERNMENTS,
            CacheValueKeyConfig.LOCAL_GOVERNMENTS_FILTERED }, allEntries = true)
    public ResponseDTO<String> delete(String code) {
        if (countyRepository.existsByLocalGovernment_Code(code)) {
            throw new InvalidException(
                    "Local Government with code " + code + " cannot be deleted because it has counties");
        }
        LocalGovernment localGovernment = localGovernmentRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("Local Government with code " + code + " not found"));
        localGovernmentRepository.delete(localGovernment);
        return new ResponseDTO<>("SUCCESS", "Local Government deleted successfully");
    }

}
