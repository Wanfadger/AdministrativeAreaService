package com.wanfadger.AdministrativeareaApi.service.localgovernment;

import com.wanfadger.AdministrativeareaApi.areaexceptions.AlreadyExistsException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.MissingDataException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.NotFoundException;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaExcelDTO;
import com.wanfadger.AdministrativeareaApi.dto.LocalGovernmentDTO;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;

import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.uniqueDtos.ULocalGovernment;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.entity.LocalGovernment;
import com.wanfadger.AdministrativeareaApi.entity.Region;
import com.wanfadger.AdministrativeareaApi.entity.SubRegion;
import com.wanfadger.AdministrativeareaApi.repository.LocalGovernmentRepository;
import com.wanfadger.AdministrativeareaApi.repository.SubRegionRepository;
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
    @CacheEvict(value = { CacheValueKeyConfig.LOCAL_GOVERNMENTS }, allEntries = true)
    public ResponseDTO<String> create(NewAdministrativeAreaDTO dto) {
        if (dto.getPartOfCode() == null || dto.getPartOfCode().isEmpty()) {
            throw new MissingDataException("Missing PartOfCode(sub region) for the local government");
        }

        SubRegion subRegion = subRegionRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                .orElseThrow(() -> new InvalidException("Invalid PartOfCode: " + dto.getPartOfCode()));

        if (localGovernmentRepository.existsByNameIgnoreCaseAndSubRegion_Code(dto.getName(), dto.getPartOfCode())) {
            throw new AlreadyExistsException("Local Government " + dto.getName() + " Already Exists in the sub region");
        }

        LocalGovernment localGovernment = localGovernmentMapperService.toLocalGovernment(dto, subRegion);

        localGovernmentRepository.save(localGovernment);

        return new ResponseDTO<>(localGovernment.getCode(),
                "successfully created a local government");
    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.LOCAL_GOVERNMENTS }, allEntries = true)
    public ResponseDTO<String> createAll(List<NewAdministrativeAreaDTO> dtos) {
        if (dtos.parallelStream().anyMatch(dto -> dto.getPartOfCode() == null || dto.getPartOfCode().isEmpty())) {
            throw new MissingDataException("Found Administrative Area without PartOfCoce");
        }

        List<String> subRegionCodes = dtos.stream()
                .map(NewAdministrativeAreaDTO::getPartOfCode)
                .collect(Collectors.toList());

        Map<String, SubRegion> subRegionMap = subRegionRepository.findByCodeIgnoreCaseIn(subRegionCodes).stream()
                .collect(Collectors.toMap(SubRegion::getCode, subRegion -> subRegion,
                        (existing, replacement) -> existing));

        List<LocalGovernment> localGovernments = dtos.parallelStream()
                .filter(dto -> !localGovernmentRepository
                        .existsByNameIgnoreCaseAndSubRegion_Code(dto.getName(), dto.getPartOfCode()))
                .filter(dto -> subRegionMap.containsKey(dto.getPartOfCode()))
                .map(dto -> {
                    SubRegion subRegion = subRegionMap.get(dto.getPartOfCode());
                    return localGovernmentMapperService.toLocalGovernment(dto, subRegion);
                })
                .toList();

        localGovernmentRepository.saveAll(localGovernments);

        return new ResponseDTO<>("success",
                "successfully added " + localGovernments.size() + " administrative areas");
    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.LOCAL_GOVERNMENTS }, allEntries = true)
    public ResponseDTO<String> update(String code, UpdateAdministrativeAreaDTO dto) {
        if (dto.getPartOfCode() == null || dto.getPartOfCode().isEmpty()) {
            throw new MissingDataException("Missing PartOfCode");
        }

        SubRegion subRegion = subRegionRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                .orElseThrow(() -> new InvalidException("Invalid PartOfCode"));
        LocalGovernment localGovernment = localGovernmentRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("Administrative Area NotFound"));

        if (dto.getName() != null && !dto.getName().isEmpty()) {
            localGovernment.setName(dto.getName());
        }

        if (dto.getLatitude() != null && !dto.getLatitude().isEmpty()) {
            localGovernment.setLatitude(Double.valueOf(dto.getLatitude()));
        }

        if (dto.getLongitude() != null && !dto.getLongitude().isEmpty()) {
            localGovernment.setLongitude(Double.valueOf(dto.getLongitude()));
        }

        if (dto.getDescription() != null && !dto.getDescription().isEmpty()) {
            localGovernment.setDescription(dto.getDescription());
        }

        localGovernment.setSubRegion(subRegion);

        localGovernmentRepository.save(localGovernment);

        return new ResponseDTO<>("SUCCESS");
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.LOCAL_GOVERNMENTS, key = "#code")
    public ResponseDTO<LocalGovernmentDTO> getByCode(String code) {
        LocalGovernment localGovernment = localGovernmentRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("LocalGovernment not found"));
        return new ResponseDTO<>(localGovernmentMapperService.toDTO(localGovernment));
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.LOCAL_GOVERNMENTS, key = "#code")
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
    public void upload(List<AdministrativeAreaExcelDTO> dtoList) {
        // Step 1: Get all existing local governments from database
        List<LocalGovernment> dbLocalGovernments = localGovernmentRepository.findAll();

        // Step 2: Extract unique new local governments
        Set<ULocalGovernment> newLocalGovernmentSet = dtoList.parallelStream()
                .filter(dto -> dbLocalGovernments.stream().noneMatch(dbLocalGovernment -> {
                    SubRegion subRegion = dto.getDbSubRegion();
                    Region region = dto.getDbRegion();

                    // Handling potential nulls
                    if (subRegion == null && dbLocalGovernment.getSubRegion() == null)
                        return dbLocalGovernment.getName().equalsIgnoreCase(dto.getLocalGovernment());
                    if (subRegion == null || dbLocalGovernment.getSubRegion() == null)
                        return false;

                    return (dbLocalGovernment.getName().equalsIgnoreCase(dto.getLocalGovernment())
                            && subRegion.getName().equalsIgnoreCase(dto.getSubRegion())
                            && region.getName().equalsIgnoreCase(dto.getRegion()));
                }))
                .map(dto -> new ULocalGovernment(dto.getLocalGovernment(), dto.getDbSubRegion().getId()))
                .collect(Collectors.toSet());

        // Step 3: Save new and fetch updated list
        List<LocalGovernment> dbLocalGovernments2;
        if (newLocalGovernmentSet.size() > 0) {
            List<LocalGovernment> newLocalGovernments = newLocalGovernmentSet.stream().map(uL -> {
                LocalGovernment localGovernment = new LocalGovernment();
                localGovernment.setCode(generateCode());
                localGovernment.setName(uL.getName());
                localGovernment.setSubRegion(subRegionRepository.findById(uL.getId())
                        .orElseThrow(() -> new NotFoundException("SubRegion not found")));
                return localGovernment;
            }).toList();
            localGovernmentRepository.saveAll(newLocalGovernments);
            dbLocalGovernments2 = localGovernmentRepository.findAll();
        } else {
            dbLocalGovernments2 = dbLocalGovernments;
        }

        // Step 4: Update DTOs
        dtoList.parallelStream().forEach(oldDto -> {
            dbLocalGovernments2.stream()
                    .filter(dbLocalGovernment -> {
                        SubRegion subRegion = dbLocalGovernment.getSubRegion();
                        Region region = subRegion.getRegion();
                        return (dbLocalGovernment.getName().equalsIgnoreCase(oldDto.getLocalGovernment())
                                && subRegion.getName().equalsIgnoreCase(oldDto.getSubRegion())
                                && region.getName().equalsIgnoreCase(oldDto.getRegion()));
                    })
                    .findFirst()
                    .ifPresent(oldDto::setDbLocalGovernment);
        });

    }

    @Override
    public Optional<LocalGovernment> findByCode(String code) {
        return localGovernmentRepository.findByCodeIgnoreCase(code);
    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.LOCAL_GOVERNMENTS }, allEntries = true)
    public void saveAll(List<LocalGovernment> localGovernments) {
        localGovernmentRepository.saveAll(localGovernments);

    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.LOCAL_GOVERNMENTS }, allEntries = true)
    public ResponseDTO<String> delete(String code) {
        LocalGovernment localGovernment = localGovernmentRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("Local Government not found"));
        localGovernmentRepository.delete(localGovernment);
        return new ResponseDTO<>("SUCCESS", "Local Government deleted successfully");
    }

    private String generateCode() {
        return sharedService.generateCode(AdministrativeAreaType.LOCALGOVERNMENT);
    }
}
