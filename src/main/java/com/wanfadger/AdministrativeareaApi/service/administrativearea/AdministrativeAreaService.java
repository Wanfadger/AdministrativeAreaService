package com.wanfadger.AdministrativeareaApi.service.administrativearea;

import com.wanfadger.AdministrativeareaApi.dto.*;
import com.wanfadger.AdministrativeareaApi.shared.reponses.AdministrativeAreaResponseDto;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface AdministrativeAreaService {

    ResponseEntity<AdministrativeAreaResponseDto<String>> newOne(Map<String , String> queryMap, NewAdministrativeAreaDto dto);
    ResponseEntity<AdministrativeAreaResponseDto<String>> newList(Map<String , String> queryMap , List<NewAdministrativeAreaDto> dtos);

    AdministrativeAreaResponseDto<CodeNameDto> filterOne(Map<String ,String> queryMap);
    AdministrativeAreaResponseDto<List<CodeNameDto>> filterList(Map<String ,String> queryMap);
    AdministrativeAreaResponseDto<List<CodeNameDto>> getParishByPartOf(Map<String, String> queryMap);

    AdministrativeAreaResponseDto<List<? extends AdministrativeAreaDto>> searchList(Map<String ,String> queryMap);
    AdministrativeAreaResponseDto<? extends AdministrativeAreaDto> searchOne(Map<String, String> queryMap);


    AdministrativeAreaResponseDto<String> upload(List<AdministrativeAreaExcelDto> administrativeAreaExcelDtos);

    AdministrativeAreaResponseDto<String> updateOne(Map<String, String> queryMap, UpdateAdministrativeAreaDto dto);



}
