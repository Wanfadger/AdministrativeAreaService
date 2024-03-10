package com.wanfadger.AdministrativeareaApi.service.administrativearea;

import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaDto;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDto;
import com.wanfadger.AdministrativeareaApi.repository.projections.CodeNameProjection;
import com.wanfadger.AdministrativeareaApi.shared.reponses.AdministrativeAreaResponseDto;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface AdministrativeAreaService {

    ResponseEntity<AdministrativeAreaResponseDto<String>> newOne(String type , NewAdministrativeAreaDto dto);
    ResponseEntity<AdministrativeAreaResponseDto<String>> newList(String type , List<NewAdministrativeAreaDto> dtos);

    AdministrativeAreaResponseDto<CodeNameProjection> filterOne(Map<String ,String> queryMap);
    AdministrativeAreaResponseDto<List<CodeNameProjection>> filterList(Map<String ,String> queryMap);

    AdministrativeAreaResponseDto<List<?>> searchList(Map<String ,String> queryMap);


}
