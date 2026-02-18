package com.wanfadger.AdministrativeareaApi.service.subcounty;

import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.SubCountyDTO;
import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaExcelDTO;
import com.wanfadger.AdministrativeareaApi.entity.SubCounty;

import java.util.List;
import java.util.Optional;

public interface SubCountyService {
    ResponseDTO<String> create(NewAdministrativeAreaDTO dto);

    ResponseDTO<String> update(String code, UpdateAdministrativeAreaDTO dto);

    ResponseDTO<String> delete(String code);

    ResponseDTO<List<SubCountyDTO>> list(String countyCode);

    ResponseDTO<SubCountyDTO> getByCode(String code);

    Optional<SubCounty> findByCode(String code);

    List<SubCounty> findAll();

    List<SubCounty> findAllByCountyCode(String countyCode);

    List<SubCounty> findAllByCountyCodes(List<String> countyCodes);

    ResponseDTO<String> createAll(List<NewAdministrativeAreaDTO> dtos);

    ResponseDTO<List<SubCountyDTO>> search(String name, String code);

    void upload(List<AdministrativeAreaExcelDTO> dtoList);

    void saveAll(List<SubCounty> subCounties);
}
