package com.wanfadger.AdministrativeareaApi.service.administrativearea;

import com.wanfadger.AdministrativeareaApi.dto.*;
import com.wanfadger.AdministrativeareaApi.shared.reponses.AdministrativeAreaResponseDto;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface AdministrativeAreaService {

    ResponseEntity<AdministrativeAreaResponseDto<String>> newOne(Map<String , String> queryMap, NewAdministrativeAreaDTO dto);
    ResponseEntity<AdministrativeAreaResponseDto<String>> newList(Map<String , String> queryMap , List<NewAdministrativeAreaDTO> dtos);

    AdministrativeAreaResponseDto<CodeNameDTO> filterOne(Map<String ,String> queryMap);
    AdministrativeAreaResponseDto<List<CodeNameDTO>> filterList(Map<String ,String> queryMap);
    AdministrativeAreaResponseDto<List<CodeNameDTO>> getParishByPartOf(Map<String, String> queryMap);

    AdministrativeAreaResponseDto<?> searchList(Map<String ,String> queryMap);
    AdministrativeAreaResponseDto<?> searchOne(Map<String, String> queryMap);


    AdministrativeAreaResponseDto<String> upload(List<AdministrativeAreaExcelDTO> administrativeAreaExcelDtos);

    AdministrativeAreaResponseDto<String> updateOne(Map<String, String> queryMap, UpdateAdministrativeAreaDTO dto);



}
