package com.wanfadger.AdministrativeareaApi.service.administrativearea;

import com.wanfadger.AdministrativeareaApi.dto.*;

import jakarta.validation.constraints.NotBlank;

import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;

import java.util.List;
import java.util.Map;

public interface AdministrativeAreaService {

        ResponseDTO<String> create(@NotBlank String type, NewAdministrativeAreaDTO dto);

        ResponseDTO<String> createList(@NotBlank String type, List<NewAdministrativeAreaDTO> dtos);

        PaginatedResponseDTO<? extends AdministrativeAreaDTO> filter(Map<String, String> queryMap);

        ResponseDTO<?> getByCode(@NotBlank String type, @NotBlank String code);

        ResponseDTO<?> getDetailsByCode(@NotBlank String type, @NotBlank String code);

        PaginatedResponseDTO<? extends AdministrativeAreaDTO> search(Map<String, String> queryMap);

        ResponseDTO<String> excelJson(List<ExcelJsonDTO> excelJsonDtos);

        ResponseDTO<String> update(@NotBlank String type, UpdateAdministrativeAreaDTO dto);

        ResponseDTO<String> delete(@NotBlank String type, @NotBlank String code);

}
