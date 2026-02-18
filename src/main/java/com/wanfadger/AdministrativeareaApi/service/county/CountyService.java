package com.wanfadger.AdministrativeareaApi.service.county;

import com.wanfadger.AdministrativeareaApi.dto.CountyDTO;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaExcelDTO;
import com.wanfadger.AdministrativeareaApi.entity.County;

import java.util.List;
import java.util.Optional;

public interface CountyService {
    ResponseDTO<String> create(NewAdministrativeAreaDTO dto);

    ResponseDTO<String> update(String code, UpdateAdministrativeAreaDTO dto);

    ResponseDTO<String> delete(String code);

    ResponseDTO<List<CountyDTO>> list(String localGovernmentCode);

    ResponseDTO<CountyDTO> getByCode(String code);

    Optional<County> findByCode(String code);

    List<County> findAll();

    List<County> findAllByLocalGovernmentCode(String localGovernmentCode);

    List<County> findAllByLocalGovernmentCodes(List<String> localGovernmentCodes);

    ResponseDTO<String> createAll(List<NewAdministrativeAreaDTO> dtos);

    ResponseDTO<List<CountyDTO>> search(String name, String code);

    void upload(List<AdministrativeAreaExcelDTO> dtoList);

    void saveAll(List<County> counties);
}
