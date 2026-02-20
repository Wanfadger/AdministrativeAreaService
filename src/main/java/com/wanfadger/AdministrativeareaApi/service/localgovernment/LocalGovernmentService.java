package com.wanfadger.AdministrativeareaApi.service.localgovernment;

import com.wanfadger.AdministrativeareaApi.dto.LocalGovernmentDTO;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.ExcelJsonDTO;
import com.wanfadger.AdministrativeareaApi.entity.LocalGovernment;
import com.wanfadger.AdministrativeareaApi.entity.SubRegion;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LocalGovernmentService {
    ResponseDTO<String> create(NewAdministrativeAreaDTO dto);

    ResponseDTO<String> update(UpdateAdministrativeAreaDTO dto);

    ResponseDTO<String> delete(String code);

    PaginatedResponseDTO<LocalGovernmentDTO> search(Map<String, String> queryMap);

    PaginatedResponseDTO<LocalGovernmentDTO> filter(Map<String, String> queryMap);

    // ResponseDTO<List<LocalGovernmentDTO>> list(String subRegionCode);

    ResponseDTO<LocalGovernmentDTO> getByCode(String code);

    ResponseDTO<LocalGovernmentDTO> findDetailsByCode(String code);

    Optional<LocalGovernment> findByCode(String code);

    ResponseDTO<String> createAll(List<NewAdministrativeAreaDTO> dtos);

    List<LocalGovernment> upload(List<ExcelJsonDTO> dtos, Map<String, SubRegion> subRegionMap);

    List<LocalGovernment> findByNames(List<String> names);

    List<LocalGovernment> saveAll(List<LocalGovernment> localGovernments);

}
