package com.wanfadger.AdministrativeareaApi.service.administrativearea;

import com.wanfadger.AdministrativeareaApi.dto.*;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;

import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface AdministrativeAreaService {

        ResponseEntity<ResponseDTO<String>> newOne(Map<String, String> queryMap,
                        NewAdministrativeAreaDTO dto);

        ResponseEntity<ResponseDTO<String>> newList(Map<String, String> queryMap,
                        List<NewAdministrativeAreaDTO> dtos);

        ResponseDTO<CodeNameDTO> filterOne(Map<String, String> queryMap);

        ResponseDTO<List<CodeNameDTO>> filterList(Map<String, String> queryMap);

        ResponseDTO<List<CodeNameDTO>> getParishByPartOf(Map<String, String> queryMap);

        ResponseDTO<?> searchList(Map<String, String> queryMap);

        ResponseDTO<?> searchOne(Map<String, String> queryMap);

        ResponseDTO<String> upload(List<AdministrativeAreaExcelDTO> administrativeAreaExcelDtos);

        ResponseDTO<String> updateOne(Map<String, String> queryMap, UpdateAdministrativeAreaDTO dto);

        ResponseDTO<String> deleteOne(Map<String, String> queryMap);

}
