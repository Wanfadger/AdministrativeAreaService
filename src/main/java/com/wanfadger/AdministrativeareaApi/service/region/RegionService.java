package com.wanfadger.AdministrativeareaApi.service.region;

import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.RegionDTO;
import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaExcelDTO;
import com.wanfadger.AdministrativeareaApi.entity.Region;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RegionService {
    ResponseDTO<String> create(NewAdministrativeAreaDTO dto);

    ResponseDTO<String> update(String code, UpdateAdministrativeAreaDTO dto);

    ResponseDTO<List<RegionDTO>> list();

    ResponseDTO<RegionDTO> getByCode(String code);

    Optional<Region> findByCode(String code);

    List<Region> findAll();

    ResponseDTO<String> createAll(List<NewAdministrativeAreaDTO> dtos);

    ResponseDTO<List<RegionDTO>> search(String name, String code);

    void upload(List<AdministrativeAreaExcelDTO> dtoList);

    ResponseDTO<String> delete(String code);

    void saveAll(List<Region> regions);

    PaginatedResponseDTO<RegionDTO> search(Map<String, String> queryMap);
}
