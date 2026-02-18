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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.wanfadger.AdministrativeareaApi.config.CacheValueKeyConfig;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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

        if (parishRepository.findByNameIgnoreCaseAndSubCounty_Code(dto.getName(), subCounty.getCode()).isPresent()) {
            throw new AlreadyExistsException("Parish Already Exists in the sub county");
        }

        Parish parish = new Parish();
        parish.setCode(generateCode());
        parish.setName(dto.getName());
        parish.setSubCounty(subCounty);

        if (dto.getLatitude() != null && !dto.getLatitude().isEmpty()) {
            parish.setLatitude(Double.valueOf(dto.getLatitude()));
        }
        if (dto.getLongitude() != null && !dto.getLongitude().isEmpty()) {
            parish.setLongitude(Double.valueOf(dto.getLongitude()));
        }
        if (dto.getDescription() != null && !dto.getDescription().isEmpty()) {
            parish.setDescription(dto.getDescription());
        }

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

        List<Parish> parishes = dtos.parallelStream()
                .map(dto -> {
                    SubCounty subCounty = subCountyRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                            .orElseThrow(() -> new InvalidException("Invalid PartOfCode: " + dto.getPartOfCode()));

                    if (parishRepository.findByNameIgnoreCaseAndSubCounty_Code(dto.getName(), subCounty.getCode())
                            .isPresent()) {
                        return null;
                    }

                    Parish parish = new Parish();
                    parish.setName(dto.getName());
                    parish.setLatitude(dto.getLatitude() != null && !dto.getLatitude().isEmpty()
                            ? Double.valueOf(dto.getLatitude())
                            : null);
                    parish.setLongitude(dto.getLongitude() != null && !dto.getLongitude().isEmpty()
                            ? Double.valueOf(dto.getLongitude())
                            : null);
                    parish.setCode(generateCode());
                    parish.setSubCounty(subCounty);
                    return parish;
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        parishRepository.saveAll(Objects.requireNonNull(parishes));

        return new ResponseDTO<>("success",
                "successfully added " + parishes.size() + " administrative areas");
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

    @Override
    @Cacheable(value = CacheValueKeyConfig.PARISHES, key = "'list:' + #subCountyCode")
    public ResponseDTO<List<ParishDTO>> list(String subCountyCode) {
        Specification<Parish> spec = Specification.where(null);
        if (subCountyCode != null && !subCountyCode.isEmpty()) {
            spec = spec.and(
                    new GenericSpecification<>(new SearchCriteria("subCounty.code", subCountyCode, MatchType.EQUALS)));
        }

        List<ParishDTO> parishDTOs = parishRepository.findAll(spec).stream()
                .map(this::convertParishDTO)
                .sorted(Comparator.comparing(ParishDTO::getCode))
                .toList();
        return new ResponseDTO<>(parishDTOs);
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.PARISHES, key = "#code")
    public ResponseDTO<ParishDTO> getByCode(String code) {
        Parish parish = parishRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("Parish not found"));
        return new ResponseDTO<>(convertParishDTO(parish));
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.PARISHES, key = "'search:' + #name + '-' + #code")
    public ResponseDTO<List<ParishDTO>> search(String name, String code) {
        Specification<Parish> spec = Specification.where(null);
        if (name != null && !name.isEmpty()) {
            spec = spec.and(new GenericSpecification<>(new SearchCriteria("name", name, MatchType.CONTAINS)));
        }
        if (code != null && !code.isEmpty()) {
            spec = spec.and(new GenericSpecification<>(new SearchCriteria("code", code, MatchType.EQUALS)));
        }

        List<ParishDTO> parishDtos = parishRepository.findAll(spec).stream()
                .map(this::convertParishDTO)
                .sorted(Comparator.comparing(ParishDTO::getCode))
                .toList();

        return new ResponseDTO<>(parishDtos);
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

    @Override
    public List<Parish> findAll() {
        return parishRepository.findAll();
    }

    @Override
    public List<Parish> findAllBySubCountyCode(String subCountyCode) {
        return parishRepository.findAll(
                new GenericSpecification<>(new SearchCriteria("subCounty.code", subCountyCode, MatchType.EQUALS)));
    }

    @Override
    public List<Parish> findAllBySubCountyCodes(List<String> subCountyCodes) {
        return parishRepository.findAll(
                new GenericSpecification<>(new SearchCriteria("subCounty.code", subCountyCodes, MatchType.IN)));
    }

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

    private ParishDTO convertParishDTO(Parish parish) {
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
