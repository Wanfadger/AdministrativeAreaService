package com.wanfadger.AdministrativeareaApi.service.subRegion;

import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.SubRegionDTO;
import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaExcelDTO;
import com.wanfadger.AdministrativeareaApi.entity.SubRegion;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SubRegionService {
    ResponseDTO<String> create(NewAdministrativeAreaDTO dto);

    ResponseDTO<String> createAll(List<NewAdministrativeAreaDTO> dtos);

    PaginatedResponseDTO<SubRegionDTO> search(Map<String, String> queryMap);

    ResponseDTO<String> update(String code, UpdateAdministrativeAreaDTO dto);

    // ResponseDTO<List<SubRegionDTO>> list(String regionCode);

    ResponseDTO<SubRegionDTO> getByCode(String code);

    Optional<SubRegion> findByCode(String code);

    // List<SubRegion> findAll();

    // List<SubRegion> findAllByRegionCode(String regionCode);

    // ResponseDTO<List<SubRegionDTO>> search(String name, String code);

    void upload(List<AdministrativeAreaExcelDTO> dtoList);

    ResponseDTO<String> delete(String code);

    void saveAll(List<SubRegion> subRegions);

}
