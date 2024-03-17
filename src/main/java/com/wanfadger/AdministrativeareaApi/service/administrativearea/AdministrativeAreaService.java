package com.wanfadger.AdministrativeareaApi.service.administrativearea;

import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaDto;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaExcelDto;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDto;
import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDto;
import com.wanfadger.AdministrativeareaApi.repository.projections.CodeNameProjection;
import com.wanfadger.AdministrativeareaApi.shared.reponses.AdministrativeAreaResponseDto;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface AdministrativeAreaService {

    ResponseEntity<AdministrativeAreaResponseDto<String>> newOne(Map<String , String> queryMap, NewAdministrativeAreaDto dto);
    ResponseEntity<AdministrativeAreaResponseDto<String>> newList(Map<String , String> queryMap , List<NewAdministrativeAreaDto> dtos);

    AdministrativeAreaResponseDto<CodeNameProjection> filterOne(Map<String ,String> queryMap);
    AdministrativeAreaResponseDto<List<CodeNameProjection>> filterList(Map<String ,String> queryMap);

    AdministrativeAreaResponseDto<List<? extends AdministrativeAreaDto>> searchList(Map<String ,String> queryMap);
    AdministrativeAreaResponseDto<? extends AdministrativeAreaDto> searchOne(Map<String, String> queryMap);


    AdministrativeAreaResponseDto<String> upload(List<AdministrativeAreaExcelDto> administrativeAreaExcelDtos);

    AdministrativeAreaResponseDto<String> updateOne(Map<String, String> queryMap, UpdateAdministrativeAreaDto dto);


}
