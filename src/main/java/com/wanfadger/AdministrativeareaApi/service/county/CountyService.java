package com.wanfadger.AdministrativeareaApi.service.county;

import com.wanfadger.AdministrativeareaApi.dto.CountyDTO;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.ExcelJsonDTO;
import com.wanfadger.AdministrativeareaApi.entity.County;
import com.wanfadger.AdministrativeareaApi.entity.LocalGovernment;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CountyService {
    ResponseDTO<String> create(NewAdministrativeAreaDTO dto);

    ResponseDTO<String> update(UpdateAdministrativeAreaDTO dto);

    ResponseDTO<String> delete(@NotBlank String code);

    ResponseDTO<CountyDTO> getByCode(@NotBlank String code);

    ResponseDTO<CountyDTO> findDetailsByCode(@NotBlank String code);

    Optional<County> findByCode(@NotBlank String code);

    ResponseDTO<String> createAll(List<NewAdministrativeAreaDTO> dtos);

    List<County> upload(List<ExcelJsonDTO> dtos, Map<String, LocalGovernment> localGovernmentMap);

    List<County> findByNames(List<String> names);

    List<County> saveAll(List<County> counties);

    PaginatedResponseDTO<CountyDTO> search(Map<String, String> queryMap);

    PaginatedResponseDTO<CountyDTO> filter(Map<String, String> queryMap);
}
