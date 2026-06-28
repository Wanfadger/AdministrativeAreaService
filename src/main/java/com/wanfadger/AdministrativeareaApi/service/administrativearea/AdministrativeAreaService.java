package com.wanfadger.AdministrativeareaApi.service.administrativearea;

import com.wanfadger.AdministrativeareaApi.dto.*;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;

import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface AdministrativeAreaService {

    ResponseEntity<ResponseDTO<String>> newOne(Map<String , String> queryMap, NewAdministrativeAreaDTO dto);
    ResponseEntity<ResponseDTO<String>> newList(Map<String , String> queryMap , List<NewAdministrativeAreaDTO> dtos);

    /**
     * Enterprise paginated search over a single administrative-area type using the
     * {@code field[:operator]=value} convention plus free-text {@code search} on
     * name/code. Requires {@code type}; honours {@code page/size/sortBy/sortDirection}
     * and an optional {@code partOf} parent-code filter. Results are cached (30 min).
     */
    PaginatedResponseDTO<AdministrativeAreaDTO> search(Map<String , String> queryMap);

    ResponseDTO<String> delete(Map<String , String> queryMap);

    ResponseDTO<CodeNameDTO> filterOne(Map<String ,String> queryMap);
    ResponseDTO<List<CodeNameDTO>> filterList(Map<String ,String> queryMap);
    ResponseDTO<List<CodeNameDTO>> getParishByPartOf(Map<String, String> queryMap);

    ResponseDTO<?> searchList(Map<String ,String> queryMap);
    ResponseDTO<?> searchOne(Map<String, String> queryMap);


    ResponseDTO<String> upload(List<AdministrativeAreaExcelDTO> administrativeAreaExcelDtos);

    ResponseDTO<String> updateOne(Map<String, String> queryMap, UpdateAdministrativeAreaDTO dto);



}
