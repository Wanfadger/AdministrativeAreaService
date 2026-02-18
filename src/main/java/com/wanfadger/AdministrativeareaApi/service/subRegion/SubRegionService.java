package com.wanfadger.AdministrativeareaApi.service.subRegion;

import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.SubRegionDTO;
import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.AdministrativeAreaResponseDto;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaExcelDTO;
import com.wanfadger.AdministrativeareaApi.entity.SubRegion;

import java.util.List;
import java.util.Optional;

public interface SubRegionService {
    AdministrativeAreaResponseDto<String> create(NewAdministrativeAreaDTO dto);

    AdministrativeAreaResponseDto<String> update(String code, UpdateAdministrativeAreaDTO dto);

    AdministrativeAreaResponseDto<List<SubRegionDTO>> list(String regionCode);

    AdministrativeAreaResponseDto<SubRegionDTO> getByCode(String code);

    Optional<SubRegion> findByCode(String code);

    List<SubRegion> findAll();

    List<SubRegion> findAllByRegionCode(String regionCode);

    AdministrativeAreaResponseDto<String> createAll(List<NewAdministrativeAreaDTO> dtos);

    AdministrativeAreaResponseDto<List<SubRegionDTO>> search(String name, String code);

    void upload(List<AdministrativeAreaExcelDTO> dtoList);

    void saveAll(List<SubRegion> subRegions);
}
