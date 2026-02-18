package com.wanfadger.AdministrativeareaApi.service.parish;

import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.ParishDTO;
import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaExcelDTO;
import com.wanfadger.AdministrativeareaApi.entity.Parish;

import java.util.List;
import java.util.Optional;

public interface ParishService {
    ResponseDTO<String> create(NewAdministrativeAreaDTO dto);

    ResponseDTO<String> update(String code, UpdateAdministrativeAreaDTO dto);

    ResponseDTO<String> delete(String code);

    ResponseDTO<List<ParishDTO>> list(String subCountyCode);

    ResponseDTO<ParishDTO> getByCode(String code);

    Optional<Parish> findByCode(String code);

    List<Parish> findAll();

    List<Parish> findAllBySubCountyCode(String subCountyCode);

    List<Parish> findAllBySubCountyCodes(List<String> subCountyCodes);

    ResponseDTO<String> createAll(List<NewAdministrativeAreaDTO> dtos);

    ResponseDTO<List<ParishDTO>> search(String name, String code);

    void upload(List<AdministrativeAreaExcelDTO> dtoList);

    void saveAll(List<Parish> parishes);
}
