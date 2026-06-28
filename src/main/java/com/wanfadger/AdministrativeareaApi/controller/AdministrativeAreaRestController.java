package com.wanfadger.AdministrativeareaApi.controller;

import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaExcelDTO;
import com.wanfadger.AdministrativeareaApi.dto.CodeNameDTO;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.UpdateAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.service.administrativearea.AdministrativeAreaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RESTful administrative-area API following the URRMS Core path convention
 * ({@code /api/v1/{resource}} with CRUD + {@code /search}). This is a single
 * type-dispatched resource: every endpoint takes an {@code AdministrativeAreaType type}
 * (rendered as a dropdown in Swagger).
 *
 * <p>Supersedes the legacy {@code /AdministrativeAreas} and {@code /AdministrativeAreas2}
 * controllers (removed).
 */
@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping("/api/v1/administrative-areas")
@Tag(name = "Administrative Areas", description = "Manage the Ugandan administrative-area hierarchy (Region › Sub-Region › Local Government › County › Sub-County › Parish)")
public class AdministrativeAreaRestController {

    private final AdministrativeAreaService administrativeAreaService;

    /** Build the service query-map from the typed parameters (null values skipped). */
    private Map<String, String> params(AdministrativeAreaType type, String... keyValues) {
        Map<String, String> map = new HashMap<>();
        map.put("type", type.name());
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            if (keyValues[i + 1] != null && !keyValues[i + 1].isBlank()) {
                map.put(keyValues[i], keyValues[i + 1]);
            }
        }
        return map;
    }

    @Operation(summary = "Create an administrative area",
            description = "For non-REGION types, the body must include 'partOfCode'.")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseDTO<String>> create(
            @Parameter(description = "Administrative area type", required = true)
            @RequestParam AdministrativeAreaType type,
            @RequestBody NewAdministrativeAreaDTO dto) {
        return administrativeAreaService.newOne(params(type), dto);
    }

    @Operation(summary = "Create multiple administrative areas", description = "Bulk-create areas of the same type.")
    @PostMapping(value = "/batch", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseDTO<String>> createBatch(
            @Parameter(description = "Administrative area type", required = true)
            @RequestParam AdministrativeAreaType type,
            @RequestBody List<NewAdministrativeAreaDTO> dtos) {
        return administrativeAreaService.newList(params(type), dtos);
    }

    @Operation(summary = "Bulk upload from Excel rows",
            description = "Hierarchical bulk upload (Region → … → Parish). Processed asynchronously.")
    @PostMapping(value = "/upload", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<String> upload(
            @RequestBody List<AdministrativeAreaExcelDTO> administrativeAreaExcelDtos) {
        return administrativeAreaService.upload(administrativeAreaExcelDtos);
    }

    @Operation(summary = "Update an administrative area by code")
    @PutMapping(value = "/{code}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<String> update(
            @Parameter(description = "Administrative area code", required = true)
            @PathVariable String code,
            @Parameter(description = "Administrative area type", required = true)
            @RequestParam AdministrativeAreaType type,
            @RequestBody UpdateAdministrativeAreaDTO dto) {
        dto.setCode(code);
        return administrativeAreaService.updateOne(params(type), dto);
    }

    @Operation(summary = "Get an administrative area (code + name) by code",
            description = "Lightweight lookup returning only code and name.")
    @GetMapping(value = "/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<CodeNameDTO> getOne(
            @Parameter(description = "Administrative area code", required = true)
            @PathVariable String code,
            @Parameter(description = "Administrative area type", required = true)
            @RequestParam AdministrativeAreaType type) {
        return administrativeAreaService.filterOne(params(type, "code", code));
    }

    @Operation(summary = "Get full details of an administrative area by code",
            description = "Returns code, name and coordinates (with parent hierarchy).")
    @GetMapping(value = "/{code}/details", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<?> getOneDetails(
            @Parameter(description = "Administrative area code", required = true)
            @PathVariable String code,
            @Parameter(description = "Administrative area type", required = true)
            @RequestParam AdministrativeAreaType type) {
        return administrativeAreaService.searchOne(params(type, "code", code));
    }

    @Operation(summary = "Paginated search",
            description = "Filter by the typed parameters below. Advanced users may also append "
                    + "field:operator=value query params (operators: EQUALS, NOT_EQUALS, CONTAINS, "
                    + "NOT_CONTAINS, GT, LT, GTE, LTE, IN), e.g. &name:CONTAINS=ka.")
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public PaginatedResponseDTO<AdministrativeAreaDTO> search(
            @Parameter(description = "Administrative area type", required = true)
            @RequestParam AdministrativeAreaType type,
            @Parameter(description = "Free-text search over name/code")
            @RequestParam(required = false) String search,
            @Parameter(description = "Parent code filter (sub-regions of a region, counties of an LG, …)")
            @RequestParam(required = false) String partOf,
            @Parameter(description = "Page number (1-based)")
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @Parameter(description = "Page size")
            @RequestParam(required = false, defaultValue = "100") Integer size,
            @Parameter(description = "Field to sort by (default: code)")
            @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction: asc | desc")
            @RequestParam(required = false) String sortDirection,
            // Captures everything (incl. advanced field:operator filters); hidden from Swagger.
            @Parameter(hidden = true) @RequestParam Map<String, String> allParams) {
        allParams.put("type", type.name());
        return administrativeAreaService.search(allParams);
    }

    @Operation(summary = "List administrative areas (code + name)",
            description = "Lightweight list by type; non-REGION types require 'partOf' (parent code).")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<List<CodeNameDTO>> list(
            @Parameter(description = "Administrative area type", required = true)
            @RequestParam AdministrativeAreaType type,
            @Parameter(description = "Parent code (required for non-REGION types)")
            @RequestParam(required = false) String partOf) {
        return administrativeAreaService.filterList(params(type, "partOf", partOf));
    }

    @Operation(summary = "List parishes under a parent area",
            description = "Returns all parishes beneath a parent (type + parent code).")
    @GetMapping(value = "/parishes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<List<CodeNameDTO>> parishesByParent(
            @Parameter(description = "Parent administrative area type", required = true)
            @RequestParam AdministrativeAreaType type,
            @Parameter(description = "Parent administrative area code", required = true)
            @RequestParam String partOfCode) {
        return administrativeAreaService.getParishByPartOf(params(type, "partOfCode", partOfCode));
    }

    @Operation(summary = "Delete an administrative area by code",
            description = "Soft-deletes the area. Fails if it still has children.")
    @DeleteMapping(value = "/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<String> delete(
            @Parameter(description = "Administrative area code", required = true)
            @PathVariable String code,
            @Parameter(description = "Administrative area type", required = true)
            @RequestParam AdministrativeAreaType type) {
        return administrativeAreaService.delete(params(type, "code", code));
    }
}
