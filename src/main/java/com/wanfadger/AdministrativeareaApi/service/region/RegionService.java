package com.wanfadger.AdministrativeareaApi.service.region;

import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.RegionDTO;
import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.AdministrativeAreaResponseDto;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaExcelDTO;
import com.wanfadger.AdministrativeareaApi.entity.Region;

import java.util.List;
import java.util.Optional;

public interface RegionService {
    AdministrativeAreaResponseDto<String> create(NewAdministrativeAreaDTO dto);

    AdministrativeAreaResponseDto<String> update(String code, UpdateAdministrativeAreaDTO dto);

    AdministrativeAreaResponseDto<List<RegionDTO>> list();

    AdministrativeAreaResponseDto<RegionDTO> getByCode(String code);

    Optional<Region> findByCode(String code);

    List<Region> findAll();

    AdministrativeAreaResponseDto<String> createAll(List<NewAdministrativeAreaDTO> dtos);

    AdministrativeAreaResponseDto<List<RegionDTO>> search(String name, String code);

    void upload(List<AdministrativeAreaExcelDTO> dtoList);

    void saveAll(List<Region> regions);
}
