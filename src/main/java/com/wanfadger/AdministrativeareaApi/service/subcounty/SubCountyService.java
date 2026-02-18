package com.wanfadger.AdministrativeareaApi.service.subcounty;

import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.SubCountyDTO;
import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.AdministrativeAreaResponseDto;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaExcelDTO;
import com.wanfadger.AdministrativeareaApi.entity.SubCounty;

import java.util.List;
import java.util.Optional;

public interface SubCountyService {
    AdministrativeAreaResponseDto<String> create(NewAdministrativeAreaDTO dto);

    AdministrativeAreaResponseDto<String> update(String code, UpdateAdministrativeAreaDTO dto);

    AdministrativeAreaResponseDto<List<SubCountyDTO>> list(String countyCode);

    AdministrativeAreaResponseDto<SubCountyDTO> getByCode(String code);

    Optional<SubCounty> findByCode(String code);

    List<SubCounty> findAll();

    List<SubCounty> findAllByCountyCode(String countyCode);

    List<SubCounty> findAllByCountyCodes(List<String> countyCodes);

    AdministrativeAreaResponseDto<String> createAll(List<NewAdministrativeAreaDTO> dtos);

    AdministrativeAreaResponseDto<List<SubCountyDTO>> search(String name, String code);

    void upload(List<AdministrativeAreaExcelDTO> dtoList);

    void saveAll(List<SubCounty> subCounties);
}
