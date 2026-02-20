package com.wanfadger.AdministrativeareaApi.service.county;

import com.wanfadger.AdministrativeareaApi.dto.CountyDTO;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.ExcelJsonDTO;
import com.wanfadger.AdministrativeareaApi.entity.County;
import com.wanfadger.AdministrativeareaApi.entity.LocalGovernment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CountyService {
    ResponseDTO<String> create(NewAdministrativeAreaDTO dto);

    ResponseDTO<String> update(UpdateAdministrativeAreaDTO dto);

    ResponseDTO<String> delete(String code);

    // ResponseDTO<List<CountyDTO>> list(String localGovernmentCode);

    ResponseDTO<CountyDTO> getByCode(String code);

    ResponseDTO<CountyDTO> findDetailsByCode(String code);

    Optional<County> findByCode(String code);

    // List<County> findAll();

    // List<County> findAllByLocalGovernmentCode(String localGovernmentCode);

    // List<County> findAllByLocalGovernmentCodes(List<String>
    // localGovernmentCodes);

    ResponseDTO<String> createAll(List<NewAdministrativeAreaDTO> dtos);

    // ResponseDTO<List<CountyDTO>> search(String name, String code);

    List<County> upload(List<ExcelJsonDTO> dtos, Map<String, LocalGovernment> localGovernmentMap);

    List<County> findByNames(List<String> names);

    List<County> saveAll(List<County> counties);

    PaginatedResponseDTO<CountyDTO> search(Map<String, String> queryMap);

    PaginatedResponseDTO<CountyDTO> filter(Map<String, String> queryMap);
}
