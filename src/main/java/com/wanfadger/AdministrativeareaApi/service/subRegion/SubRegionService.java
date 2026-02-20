package com.wanfadger.AdministrativeareaApi.service.subRegion;

import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.SubRegionDTO;
import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaExcelDTO;
import com.wanfadger.AdministrativeareaApi.dto.ExcelJsonDTO;
import com.wanfadger.AdministrativeareaApi.entity.Region;
import com.wanfadger.AdministrativeareaApi.entity.SubRegion;

import java.util.List;
import java.util.Map;

public interface SubRegionService {
    ResponseDTO<String> create(NewAdministrativeAreaDTO dto);

    ResponseDTO<String> createAll(List<NewAdministrativeAreaDTO> dtos);

    PaginatedResponseDTO<SubRegionDTO> search(Map<String, String> queryMap);

    PaginatedResponseDTO<SubRegionDTO> filter(Map<String, String> queryMap);

    ResponseDTO<SubRegionDTO> findByCode(String code);

    ResponseDTO<SubRegionDTO> findDetailsByCode(String code);

    ResponseDTO<String> update(UpdateAdministrativeAreaDTO dto);

    List<SubRegion> upload(List<ExcelJsonDTO> dtos , Map<String, Region> regionMap);

    ResponseDTO<String> delete(String code);


}
