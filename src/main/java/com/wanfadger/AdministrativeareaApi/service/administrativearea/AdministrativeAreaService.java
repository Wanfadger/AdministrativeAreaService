package com.wanfadger.AdministrativeareaApi.service.administrativearea;

import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaDtos;
import com.wanfadger.AdministrativeareaApi.shared.reponses.AdministrativeAreaResponseDto;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface AdministrativeAreaService {

    ResponseEntity<AdministrativeAreaResponseDto<String>> newOne(AdministrativeAreaDtos.NewAdministrativeAreaDto dto);

    AdministrativeAreaResponseDto<AdministrativeAreaDtos.AdministrativeAreaDto> filterOne(Map<String ,String> queryMap);
    AdministrativeAreaResponseDto<List<AdministrativeAreaDtos.AdministrativeAreaDto>> filterList(Map<String ,String> queryMap);

    ResponseEntity<AdministrativeAreaResponseDto<String>> newList(List<AdministrativeAreaDtos.NewAdministrativeAreaDto> dtos);
}
