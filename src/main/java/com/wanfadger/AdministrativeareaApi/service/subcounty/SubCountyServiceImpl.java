package com.wanfadger.AdministrativeareaApi.service.subcounty;

import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.AlreadyExistsException;
import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.MissingDataException;
import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.NotFoundException;

import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaExcelDTO;
import com.wanfadger.AdministrativeareaApi.dto.CountyDTO;
import com.wanfadger.AdministrativeareaApi.dto.LocalGovernmentDTO;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.RegionDTO;
import com.wanfadger.AdministrativeareaApi.dto.SubCountyDTO;
import com.wanfadger.AdministrativeareaApi.dto.SubRegionDTO;
import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.AdministrativeAreaResponseDto;
import com.wanfadger.AdministrativeareaApi.dto.uniqueDtos.USubCounty;
import com.wanfadger.AdministrativeareaApi.entity.County;
import com.wanfadger.AdministrativeareaApi.entity.LocalGovernment;
import com.wanfadger.AdministrativeareaApi.entity.Region;
import com.wanfadger.AdministrativeareaApi.entity.SubCounty;
import com.wanfadger.AdministrativeareaApi.entity.SubRegion;
import com.wanfadger.AdministrativeareaApi.repository.CountyRepository;
import com.wanfadger.AdministrativeareaApi.repository.SubCountyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubCountyServiceImpl implements SubCountyService {

    private final SubCountyRepository subCountyRepository;
    private final CountyRepository countyRepository;

    @Override
    @Transactional
    public AdministrativeAreaResponseDto<String> create(NewAdministrativeAreaDTO dto) {
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

        return new AdministrativeAreaResponseDto<>(subCounty.getCode(), "successfully created a sub county");
    }

    @Override
    @Transactional
    public AdministrativeAreaResponseDto<String> createAll(List<NewAdministrativeAreaDTO> dtos) {
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

        return new AdministrativeAreaResponseDto<>("success",
                "successfully added " + subCounties.size() + " administrative areas");
    }

    @Override
    @Transactional
    public AdministrativeAreaResponseDto<String> update(String code, UpdateAdministrativeAreaDTO dto) {
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

        return new AdministrativeAreaResponseDto<>("SUCCESS");
    }

    @Override
    public AdministrativeAreaResponseDto<List<SubCountyDTO>> list(String countyCode) {
        List<SubCountyDTO> subCountyDTOs;
        if (countyCode != null && !countyCode.isEmpty()) {
            subCountyDTOs = subCountyRepository.findAllByCounty_Code(countyCode).stream()
                    .map(this::convertSubCountyDTO)
                    .sorted(Comparator.comparing(SubCountyDTO::getCode))
                    .toList();
        } else {
            subCountyDTOs = subCountyRepository.findAll().stream()
                    .map(this::convertSubCountyDTO)
                    .sorted(Comparator.comparing(SubCountyDTO::getCode))
                    .toList();
        }
        return new AdministrativeAreaResponseDto<>(subCountyDTOs);
    }

    @Override
    public AdministrativeAreaResponseDto<SubCountyDTO> getByCode(String code) {
        SubCounty subCounty = subCountyRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("SubCounty not found"));
        return new AdministrativeAreaResponseDto<>(convertSubCountyDTO(subCounty));
    }

    @Override
    public AdministrativeAreaResponseDto<List<SubCountyDTO>> search(String name, String code) {
        SubCounty subCounty = new SubCounty();
        if (name != null && !name.isEmpty()) {
            subCounty.setName(name);
        }
        if (code != null && !code.isEmpty()) {
            subCounty.setCode(code);
        }

        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreCase()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING);

        Example<SubCounty> example = Example.of(subCounty, matcher);
        List<SubCountyDTO> subCountyDtos = subCountyRepository.findAll(example).stream()
                .map(this::convertSubCountyDTO)
                .sorted(Comparator.comparing(SubCountyDTO::getCode))
                .toList();

        return new AdministrativeAreaResponseDto<>(subCountyDtos);
    }

    @Override
    @Transactional
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
                .map(dto -> new USubCounty(dto.getSubCounty(), dto.getDbCounty()))
                .collect(Collectors.toSet());

        List<SubCounty> dbSubCounties2;
        if (newSubCountySet.size() > 0) {
            List<SubCounty> newSubCounties = newSubCountySet.stream().map(uSC -> {
                SubCounty subCounty = new SubCounty();
                subCounty.setCode(generateCode());
                subCounty.setName(uSC.name());
                subCounty.setCounty(uSC.county());
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
        return subCountyRepository.findAllByCounty_Code(countyCode);
    }

    @Override
    public List<SubCounty> findAllByCountyCodes(List<String> countyCodes) {
        return subCountyRepository.findAllByCountyCodes(countyCodes);
    }

    @Override
    @Transactional
    public void saveAll(List<SubCounty> subCounties) {
        subCountyRepository.saveAll(subCounties);

    }

    private String generateCode() {
        String code;
        do {
            code = UUID.randomUUID().toString();
        } while (subCountyRepository.findByCodeIgnoreCase(code).isPresent());
        return code;
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
