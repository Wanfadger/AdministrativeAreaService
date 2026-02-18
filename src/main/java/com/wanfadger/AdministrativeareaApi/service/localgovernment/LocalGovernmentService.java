package com.wanfadger.AdministrativeareaApi.service.localgovernment;

import com.wanfadger.AdministrativeareaApi.dto.LocalGovernmentDTO;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.AdministrativeAreaResponseDto;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaExcelDTO;
import com.wanfadger.AdministrativeareaApi.entity.LocalGovernment;

import java.util.List;
import java.util.Optional;

public interface LocalGovernmentService {
    AdministrativeAreaResponseDto<String> create(NewAdministrativeAreaDTO dto);

    AdministrativeAreaResponseDto<String> update(String code, UpdateAdministrativeAreaDTO dto);

    AdministrativeAreaResponseDto<List<LocalGovernmentDTO>> list(String subRegionCode);

    AdministrativeAreaResponseDto<LocalGovernmentDTO> getByCode(String code);

    Optional<LocalGovernment> findByCode(String code);

    List<LocalGovernment> findAll();

    List<LocalGovernment> findAllBySubRegionCode(String subRegionCode);

    List<LocalGovernment> findAllBySubRegionCodes(List<String> subRegionCodes);

    AdministrativeAreaResponseDto<String> createAll(List<NewAdministrativeAreaDTO> dtos);

    AdministrativeAreaResponseDto<List<LocalGovernmentDTO>> search(String name, String code);

    void upload(List<AdministrativeAreaExcelDTO> dtoList);

    void saveAll(List<LocalGovernment> localGovernments);
}
