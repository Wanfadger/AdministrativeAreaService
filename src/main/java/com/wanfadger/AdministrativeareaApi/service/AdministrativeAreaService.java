package com.wanfadger.AdministrativeareaApi.service;

import com.wanfadger.AdministrativeareaApi.dto.*;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;

import java.util.List;
import java.util.Map;

public interface AdministrativeAreaService {

    /**
     * Create one or more administrative areas of the same {@code type} (always a list — a single
     * create is just a one-element list). Returns the generated code(s).
     */
    ResponseDTO<List<String>> create(Map<String, String> queryMap, List<NewAdministrativeAreaDTO> dtos);

    /**
     * Enterprise paginated search over a single administrative-area type using the
     * {@code field[:operator]=value} convention plus free-text {@code search} on
     * name/code. Requires {@code type}; honours {@code page/size/sortBy/sortDirection}
     * and an optional {@code partOf} parent-code filter. Each result carries its full
     * parent hierarchy (e.g. parish → sub-county → … → region). Results are cached (30 min).
     */
    PaginatedResponseDTO<AdministrativeAreaDTO> search(Map<String, String> queryMap);

    /** Fetch a single area by {@code code} with its full parent hierarchy. */
    ResponseDTO<AdministrativeAreaDTO> getOne(Map<String, String> queryMap);

    ResponseDTO<String> updateOne(Map<String, String> queryMap, NewAdministrativeAreaDTO dto);

    ResponseDTO<String> delete(Map<String, String> queryMap);
}
