package com.wanfadger.AdministrativeareaApi.service.region;

import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.RegionDTO;
import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaExcelDTO;

import java.util.List;
import java.util.Map;

public interface RegionService {
    ResponseDTO<String> create(NewAdministrativeAreaDTO dto);

    ResponseDTO<String> createList(List<NewAdministrativeAreaDTO> dtos);

    PaginatedResponseDTO<RegionDTO> search(Map<String, String> queryMap);

    ResponseDTO<String> update(UpdateAdministrativeAreaDTO dto);

    ResponseDTO<RegionDTO> findByCode(String code);

    ResponseDTO<RegionDTO> findDetailsByCode(String code);

    void upload(List<AdministrativeAreaExcelDTO> dtoList);

    ResponseDTO<String> delete(String code);

}
