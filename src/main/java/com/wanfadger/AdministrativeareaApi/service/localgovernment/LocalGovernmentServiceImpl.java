package com.wanfadger.AdministrativeareaApi.service.localgovernment;

import com.wanfadger.AdministrativeareaApi.areaexceptions.AlreadyExistsException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.MissingDataException;
import com.wanfadger.AdministrativeareaApi.areaexceptions.NotFoundException;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaExcelDTO;
import com.wanfadger.AdministrativeareaApi.dto.LocalGovernmentDTO;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.RegionDTO;
import com.wanfadger.AdministrativeareaApi.dto.SubRegionDTO;
import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
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
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
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

    @Override
    @Transactional
    public ResponseDTO<String> create(NewAdministrativeAreaDTO dto) {
        if (dto.getPartOfCode() == null || dto.getPartOfCode().isEmpty()) {
            throw new MissingDataException("Missing PartOfCode(sub region) for the local government");
        }

        SubRegion subRegion = subRegionRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                .orElseThrow(() -> new InvalidException("Invalid PartOfCode: " + dto.getPartOfCode()));

        if (localGovernmentRepository.findByNameIgnoreCaseAndSubRegion_Code(dto.getName(), dto.getPartOfCode())
                .isPresent()) {
            throw new AlreadyExistsException("Local Government Already Exists in the sub region");
        }

        LocalGovernment localGovernment = new LocalGovernment();
        localGovernment.setCode(generateCode());
        localGovernment.setName(dto.getName());
        localGovernment.setSubRegion(subRegion);

        if (dto.getLatitude() != null && !dto.getLatitude().isEmpty()) {
            localGovernment.setLatitude(Double.valueOf(dto.getLatitude()));
        }
        if (dto.getLongitude() != null && !dto.getLongitude().isEmpty()) {
            localGovernment.setLongitude(Double.valueOf(dto.getLongitude()));
        }
        if (dto.getDescription() != null && !dto.getDescription().isEmpty()) {
            localGovernment.setDescription(dto.getDescription());
        }

        localGovernmentRepository.save(localGovernment);

        return new ResponseDTO<>(localGovernment.getCode(),
                "successfully created a local government");
    }

    @Override
    @Transactional
    public ResponseDTO<String> createAll(List<NewAdministrativeAreaDTO> dtos) {
        if (dtos.parallelStream().anyMatch(dto -> dto.getPartOfCode() == null || dto.getPartOfCode().isEmpty())) {
            throw new MissingDataException("Found Administrative Area without PartOfCoce");
        }

        List<LocalGovernment> localGovernments = dtos.parallelStream()
                .filter(dto -> localGovernmentRepository
                        .findByNameIgnoreCaseAndSubRegion_Code(dto.getName(), dto.getPartOfCode()).isEmpty())
                .map(dto -> {
                    LocalGovernment localGovernment = new LocalGovernment();
                    localGovernment.setName(dto.getName());
                    localGovernment.setLatitude(dto.getLatitude() != null && !dto.getLatitude().isEmpty()
                            ? Double.valueOf(dto.getLatitude())
                            : null);
                    localGovernment.setLongitude(dto.getLongitude() != null && !dto.getLongitude().isEmpty()
                            ? Double.valueOf(dto.getLongitude())
                            : null);
                    localGovernment.setCode(generateCode());

                    SubRegion subRegion = subRegionRepository.findByCodeIgnoreCase(dto.getPartOfCode())
                            .orElseThrow(() -> new InvalidException("Invalid PartOfCode: " + dto.getPartOfCode()));
                    localGovernment.setSubRegion(subRegion);

                    return localGovernment;
                })
                .toList();

        localGovernmentRepository.saveAll(localGovernments);

        return new ResponseDTO<>("success",
                "successfully added " + localGovernments.size() + " administrative areas");
    }

    @Override
    @Transactional
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
    public ResponseDTO<List<LocalGovernmentDTO>> list(String subRegionCode) {
        List<LocalGovernmentDTO> localGovernmentDtos;
        if (subRegionCode != null && !subRegionCode.isEmpty()) {
            localGovernmentDtos = localGovernmentRepository.findAllBySubRegion_Code(subRegionCode).stream()
                    .map(this::convertLocalGovernmentDTO)
                    .sorted(Comparator.comparing(LocalGovernmentDTO::getCode))
                    .toList();
        } else {
            localGovernmentDtos = localGovernmentRepository.findAll().stream()
                    .map(this::convertLocalGovernmentDTO)
                    .sorted(Comparator.comparing(LocalGovernmentDTO::getCode))
                    .toList();
        }
        return new ResponseDTO<>(localGovernmentDtos);
    }

    @Override
    public ResponseDTO<LocalGovernmentDTO> getByCode(String code) {
        LocalGovernment localGovernment = localGovernmentRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("LocalGovernment not found"));
        return new ResponseDTO<>(convertLocalGovernmentDTO(localGovernment));
    }

    @Override
    public ResponseDTO<List<LocalGovernmentDTO>> search(String name, String code) {
        LocalGovernment localGovernment = new LocalGovernment();
        if (name != null && !name.isEmpty()) {
            localGovernment.setName(name);
        }
        if (code != null && !code.isEmpty()) {
            localGovernment.setCode(code);
        }

        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreCase()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING);

        Example<LocalGovernment> example = Example.of(localGovernment, matcher);
        List<LocalGovernmentDTO> localGovernmentDtos = localGovernmentRepository.findAll(example).stream()
                .map(this::convertLocalGovernmentDTO)
                .sorted(Comparator.comparing(LocalGovernmentDTO::getCode))
                .toList();

        return new ResponseDTO<>(localGovernmentDtos);
    }

    @Override
    @Transactional
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
    public List<LocalGovernment> findAll() {
        return localGovernmentRepository.findAll();
    }

    @Override
    public List<LocalGovernment> findAllBySubRegionCode(String subRegionCode) {
        return localGovernmentRepository.findAllBySubRegion_Code(subRegionCode);
    }

    @Override
    public List<LocalGovernment> findAllBySubRegionCodes(List<String> subRegionCodes) {
        return localGovernmentRepository.findAllBySubRegionCodes(subRegionCodes);
    }

    @Override
    @Transactional
    public void saveAll(List<LocalGovernment> localGovernments) {
        localGovernmentRepository.saveAll(localGovernments);

    }

    @Override
    @Transactional
    public ResponseDTO<String> delete(String code) {
        LocalGovernment localGovernment = localGovernmentRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("Local Government not found"));
        localGovernmentRepository.delete(localGovernment);
        return new ResponseDTO<>("SUCCESS", "Local Government deleted successfully");
    }

    private String generateCode() {
        return sharedService.generateCode(AdministrativeAreaType.LOCALGOVERNMENT);
    }

    private LocalGovernmentDTO convertLocalGovernmentDTO(LocalGovernment localGovernment) {
        LocalGovernmentDTO dto = new LocalGovernmentDTO();
        dto.setId(localGovernment.getId());
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
