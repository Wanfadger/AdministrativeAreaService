package com.wanfadger.AdministrativeareaApi.service.county;

import com.wanfadger.AdministrativeareaApi.areaexceptions.AlreadyExistsException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.MissingDataException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.NotFoundException;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaExcelDTO;
import com.wanfadger.AdministrativeareaApi.dto.CountyDTO;
import com.wanfadger.AdministrativeareaApi.dto.LocalGovernmentDTO;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.RegionDTO;
import com.wanfadger.AdministrativeareaApi.dto.SubRegionDTO;
import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.uniqueDtos.UCounty;
import com.wanfadger.AdministrativeareaApi.entity.County;
import com.wanfadger.AdministrativeareaApi.entity.LocalGovernment;
import com.wanfadger.AdministrativeareaApi.entity.Region;
import com.wanfadger.AdministrativeareaApi.entity.SubRegion;
import com.wanfadger.AdministrativeareaApi.repository.CountyRepository;
import com.wanfadger.AdministrativeareaApi.repository.LocalGovernmentRepository;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
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
public class CountyServiceImpl implements CountyService {

    private final CountyRepository countyRepository;
    private final LocalGovernmentRepository localGovernmentRepository;
    private final SharedService sharedService;

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.COUNTIES }, allEntries = true)
    public ResponseDTO<String> create(NewAdministrativeAreaDTO dto) {
        if (dto.getPartOfCode() == null || dto.getPartOfCode().isEmpty()) {
            throw new MissingDataException("Missing PartOfCode(local government) for the county");
        }

        LocalGovernment localGovernment = localGovernmentRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                .orElseThrow(() -> new InvalidException("Invalid PartOfCode: " + dto.getPartOfCode()));

        if (countyRepository.findByNameIgnoreCaseAndLocalGovernment_Code(dto.getName(), dto.getPartOfCode())
                .isPresent()) {
            throw new AlreadyExistsException("County Already Exists in the local government");
        }

        County county = new County();
        county.setCode(generateCode());
        county.setName(dto.getName());
        county.setLocalGovernment(localGovernment);

        if (dto.getLatitude() != null && !dto.getLatitude().isEmpty()) {
            county.setLatitude(Double.valueOf(dto.getLatitude()));
        }
        if (dto.getLongitude() != null && !dto.getLongitude().isEmpty()) {
            county.setLongitude(Double.valueOf(dto.getLongitude()));
        }
        if (dto.getDescription() != null && !dto.getDescription().isEmpty()) {
            county.setDescription(dto.getDescription());
        }

        countyRepository.save(county);

        return new ResponseDTO<>(county.getCode(), "successfully created a county");
    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.COUNTIES }, allEntries = true)
    public ResponseDTO<String> createAll(List<NewAdministrativeAreaDTO> dtos) {
        if (dtos.parallelStream().anyMatch(dto -> dto.getPartOfCode() == null || dto.getPartOfCode().isEmpty())) {
            throw new MissingDataException("Found Administrative Area without PartOfCoce");
        }

        List<County> counties = dtos.parallelStream()
                .filter(dto -> countyRepository
                        .findByNameIgnoreCaseAndLocalGovernment_Code(dto.getName(), dto.getPartOfCode()).isEmpty())
                .map(dto -> {
                    County county = new County();
                    county.setName(dto.getName());
                    county.setLatitude(dto.getLatitude() != null && !dto.getLatitude().isEmpty()
                            ? Double.valueOf(dto.getLatitude())
                            : null);
                    county.setLongitude(dto.getLongitude() != null && !dto.getLongitude().isEmpty()
                            ? Double.valueOf(dto.getLongitude())
                            : null);
                    county.setCode(generateCode());

                    LocalGovernment localGovernment = localGovernmentRepository
                            .findByCodeIgnoreCase(dto.getPartOfCode())
                            .orElseThrow(() -> new InvalidException("Invalid PartOfCode: " + dto.getPartOfCode()));
                    county.setLocalGovernment(localGovernment);

                    return county;
                })
                .toList();

        countyRepository.saveAll(Objects.requireNonNull(counties));

        return new ResponseDTO<>("success",
                "successfully added " + counties.size() + " administrative areas");
    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.COUNTIES }, allEntries = true)
    public ResponseDTO<String> update(String code, UpdateAdministrativeAreaDTO dto) {
        if (dto.getPartOfCode() == null || dto.getPartOfCode().isEmpty()) {
            throw new MissingDataException("Missing PartOfCode");
        }

        LocalGovernment localGovernment = localGovernmentRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                .orElseThrow(() -> new InvalidException("Invalid PartOfCode"));
        County county = countyRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("Administrative Area NotFound"));

        if (dto.getName() != null && !dto.getName().isEmpty()) {
            county.setName(dto.getName());
        }

        if (dto.getLatitude() != null && !dto.getLatitude().isEmpty()) {
            county.setLatitude(Double.valueOf(dto.getLatitude()));
        }

        if (dto.getLongitude() != null && !dto.getLongitude().isEmpty()) {
            county.setLongitude(Double.valueOf(dto.getLongitude()));
        }

        if (dto.getDescription() != null && !dto.getDescription().isEmpty()) {
            county.setDescription(dto.getDescription());
        }

        county.setLocalGovernment(localGovernment);

        countyRepository.save(county);

        return new ResponseDTO<>("SUCCESS");
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.COUNTIES, key = "'list:' + #localGovernmentCode")
    public ResponseDTO<List<CountyDTO>> list(String localGovernmentCode) {
        Specification<County> spec = Specification.where(null);
        if (localGovernmentCode != null && !localGovernmentCode.isEmpty()) {
            spec = spec.and(new GenericSpecification<>(
                    new SearchCriteria("localGovernment.code", localGovernmentCode, MatchType.EQUALS)));
        }

        List<CountyDTO> countyDtos = countyRepository.findAll(spec).stream()
                .map(this::convertCountyDTO)
                .sorted(Comparator.comparing(CountyDTO::getCode))
                .toList();
        return new ResponseDTO<>(countyDtos);
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.COUNTIES, key = "#code")
    public ResponseDTO<CountyDTO> getByCode(String code) {
        County county = countyRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("County not found"));
        return new ResponseDTO<>(convertCountyDTO(county));
    }

    @Override
    @Cacheable(value = CacheValueKeyConfig.COUNTIES, key = "'search:' + #name + '-' + #code")
    public ResponseDTO<List<CountyDTO>> search(String name, String code) {
        Specification<County> spec = Specification.where(null);
        if (name != null && !name.isEmpty()) {
            spec = spec.and(new GenericSpecification<>(new SearchCriteria("name", name, MatchType.CONTAINS)));
        }
        if (code != null && !code.isEmpty()) {
            spec = spec.and(new GenericSpecification<>(new SearchCriteria("code", code, MatchType.EQUALS)));
        }

        List<CountyDTO> countyDtos = countyRepository.findAll(spec).stream()
                .map(this::convertCountyDTO)
                .sorted(Comparator.comparing(CountyDTO::getCode))
                .toList();

        return new ResponseDTO<>(countyDtos);
    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.COUNTIES }, allEntries = true)
    public void upload(List<AdministrativeAreaExcelDTO> dtoList) {
        List<County> dbCounties = countyRepository.findAll();

        Set<UCounty> newCountSet = dtoList.parallelStream()
                .filter(dto -> dbCounties.stream().parallel().noneMatch(dbCounty -> {
                    LocalGovernment localGovernment = dbCounty.getLocalGovernment();
                    SubRegion subRegion = localGovernment.getSubRegion();
                    Region region = subRegion.getRegion();
                    return (dbCounty.getName().equalsIgnoreCase(dto.getCounty())
                            && localGovernment.getName().equalsIgnoreCase(dto.getLocalGovernment())
                            && subRegion.getName().equalsIgnoreCase(dto.getSubRegion())
                            && region.getName().equalsIgnoreCase(dto.getRegion()));
                })).map(dto -> new UCounty(dto.getCounty(), dto.getDbLocalGovernment().getId()))
                .collect(Collectors.toSet());

        List<County> dbCounties2;
        if (newCountSet.size() > 0) {
            List<County> newCounties = newCountSet.stream().map(UC -> {
                County county = new County();
                county.setCode(generateCode());
                county.setName(UC.getName());
                county.setLocalGovernment(localGovernmentRepository.findById(UC.getId())
                        .orElseThrow(() -> new NotFoundException("LocalGovernment not found")));
                return county;
            }).toList();
            countyRepository.saveAll(newCounties);
            dbCounties2 = countyRepository.findAll();
        } else {
            dbCounties2 = dbCounties;
        }

        dtoList.parallelStream().forEach(oldDto -> {
            dbCounties2.stream()
                    .filter(dbCounty -> {
                        LocalGovernment localGovernment = dbCounty.getLocalGovernment();
                        SubRegion subRegion = localGovernment.getSubRegion();
                        Region region = subRegion.getRegion();
                        return (dbCounty.getName().equalsIgnoreCase(oldDto.getCounty())
                                && localGovernment.getName().equalsIgnoreCase(oldDto.getLocalGovernment())
                                && subRegion.getName().equalsIgnoreCase(oldDto.getSubRegion())
                                && region.getName().equalsIgnoreCase(oldDto.getRegion()));
                    })
                    .findFirst()
                    .ifPresent(oldDto::setDbCounty);
        });

    }

    @Override
    public Optional<County> findByCode(String code) {
        return countyRepository.findByCodeIgnoreCase(code);
    }

    @Override
    public List<County> findAll() {
        return countyRepository.findAll();
    }

    @Override
    public List<County> findAllByLocalGovernmentCode(String localGovernmentCode) {
        return countyRepository.findAll(new GenericSpecification<>(
                new SearchCriteria("localGovernment.code", localGovernmentCode, MatchType.EQUALS)));
    }

    @Override
    public List<County> findAllByLocalGovernmentCodes(List<String> localGovernmentCodes) {
        return countyRepository.findAll(new GenericSpecification<>(
                new SearchCriteria("localGovernment.code", localGovernmentCodes, MatchType.IN)));
    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.COUNTIES }, allEntries = true)
    public void saveAll(List<County> counties) {
        countyRepository.saveAll(counties);

    }

    @Override
    @Transactional
    @CacheEvict(value = { CacheValueKeyConfig.COUNTIES }, allEntries = true)
    public ResponseDTO<String> delete(String code) {
        County county = countyRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("County not found"));
        countyRepository.delete(county);
        return new ResponseDTO<>("SUCCESS", "County deleted successfully");
    }

    private String generateCode() {
        return sharedService.generateCode(AdministrativeAreaType.COUNTY);
    }

    private CountyDTO convertCountyDTO(County county) {
        CountyDTO dto = new CountyDTO();
        dto.setId(county.getId());
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
