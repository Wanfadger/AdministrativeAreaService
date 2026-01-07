package com.wanfadger.AdministrativeareaApi.controller;

import com.wanfadger.AdministrativeareaApi.dto.*;
import com.wanfadger.AdministrativeareaApi.service.administrativearea.AdministrativeAreaService;
import com.wanfadger.AdministrativeareaApi.shared.reponses.AdministrativeAreaResponseDto;
import com.wanfadger.AdministrativeareaApi.shared.util.CacheKeys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin()
@RequestMapping("/AdministrativeAreas")
@Tag(name = "Administrative Areas", description = "API for managing Ugandan administrative areas (Region, Sub-Region, Local Government, County, Sub-County, Parish)")
public class AdministrativeAreaController {

    private final AdministrativeAreaService administrativeAreaService;

    @Operation(
            summary = "Create a single administrative area",
            description = "Creates a new administrative area. Requires 'type' query parameter to specify the administrative area type (REGION, SUB REGION, LOCAL GOVERNMENT, COUNTY, SUB COUNTY, PARISH). Cache is automatically evicted after creation."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Administrative area created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = "{\"data\":\"Success\",\"message\":\"Administrative area created\",\"status\":true}"))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/one", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @CacheEvict(value = {CacheKeys.ADMINISTRATIVE_AREAS , CacheKeys.ADMINISTRATIVE_AREAS_FILTER}, allEntries = true)
    public ResponseEntity<AdministrativeAreaResponseDto<String>> newOne(
            @Parameter(description = "Query parameters: 'type' (required) - Administrative area type", required = true, example = "type=REGION")
            @RequestParam Map<String, String> queryMap, 
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Administrative area data", required = true)
            @RequestBody NewAdministrativeAreaDto dto) {
        return administrativeAreaService.newOne(queryMap, dto);
    }

    @Operation(
            summary = "Create multiple administrative areas",
            description = "Creates multiple administrative areas in a single request. Requires 'type' query parameter. Cache is automatically evicted after creation."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Administrative areas created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @CacheEvict(value = {CacheKeys.ADMINISTRATIVE_AREAS , CacheKeys.ADMINISTRATIVE_AREAS_FILTER}, allEntries = true)
    public ResponseEntity<AdministrativeAreaResponseDto<String>> newList(
            @Parameter(description = "Query parameters: 'type' (required) - Administrative area type", required = true)
            @RequestParam Map<String, String> queryMap, 
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "List of administrative areas to create", required = true)
            @RequestBody List<NewAdministrativeAreaDto> dtos) {
        return administrativeAreaService.newList(queryMap, dtos);
    }

    @Operation(
            summary = "Bulk upload administrative areas from Excel",
            description = "Uploads administrative areas in bulk from Excel format. Supports hierarchical data (Region → Sub-Region → Local Government → County → Sub-County → Parish). Cache is automatically evicted after upload."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Upload completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid Excel data format"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/upload", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @CacheEvict(value = {CacheKeys.ADMINISTRATIVE_AREAS , CacheKeys.ADMINISTRATIVE_AREAS_FILTER}, allEntries = true)
    public AdministrativeAreaResponseDto<String> upload(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "List of administrative areas in Excel format", required = true)
            @RequestBody List<AdministrativeAreaExcelDto> administrativeAreaExcelDtos) {
        return administrativeAreaService.upload(administrativeAreaExcelDtos);
    }


    @Operation(
            summary = "Update an administrative area",
            description = "Updates an existing administrative area. Requires 'type' query parameter and 'code' in the request body. Cache is automatically evicted after update."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Administrative area updated successfully"),
            @ApiResponse(responseCode = "404", description = "Administrative area not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping(value = "/one", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @CacheEvict(value = {CacheKeys.ADMINISTRATIVE_AREAS , CacheKeys.ADMINISTRATIVE_AREAS_FILTER}, allEntries = true)
    public AdministrativeAreaResponseDto<String> updateOne(
            @Parameter(description = "Query parameters: 'type' (required) - Administrative area type", required = true)
            @RequestParam Map<String, String> queryMap, 
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated administrative area data (must include 'code')", required = true)
            @RequestBody UpdateAdministrativeAreaDto dto) {
        return administrativeAreaService.updateOne(queryMap, dto);
    }


    @Operation(
            summary = "Get a single administrative area (code and name only)",
            description = "Retrieves a single administrative area by type and code. Returns only code and name. Results are cached for 7 days. Query parameters: 'type' (required), 'code' (required), optionally 'partOf' for hierarchical queries."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Administrative area found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AdministrativeAreaResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Administrative area not found"),
            @ApiResponse(responseCode = "400", description = "Missing required parameters")
    })
    @GetMapping(value = "/filterOne", produces = MediaType.APPLICATION_JSON_VALUE)
    public AdministrativeAreaResponseDto<CodeNameDto> filterOne(
            @Parameter(description = "Query parameters: 'type' (required), 'code' (required), 'partOf' (optional)", required = true, example = "type=REGION&code=001")
            @RequestParam Map<String, String> queryMap) {
        return administrativeAreaService.filterOne(queryMap);
    }


    @Operation(
            summary = "Get a list of administrative areas (code and name only)",
            description = "Retrieves a list of administrative areas by type. Returns only code and name. Results are cached for 7 days. Query parameters: 'type' (required), optionally 'partOf' for filtering by parent area."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of administrative areas",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AdministrativeAreaResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Missing required parameters")
    })
    @GetMapping(value = "/filterList", produces = MediaType.APPLICATION_JSON_VALUE)
    public AdministrativeAreaResponseDto<List<CodeNameDto>> filterList(
            @Parameter(description = "Query parameters: 'type' (required), 'partOf' (optional)", required = true, example = "type=REGION")
            @RequestParam Map<String, String> queryMap ) {
        return administrativeAreaService.filterList(queryMap);
    }

    @Operation(
            summary = "Get parishes by parent administrative area",
            description = "Retrieves all parishes that belong to a parent administrative area (Region, Sub-Region, Local Government, County, or Sub-County). Returns only code and name. Results are cached for 7 days. Query parameters: 'type' (required - parent type), 'partOfCode' (required - parent code)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of parishes",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "400", description = "Missing required parameters")
    })
    @GetMapping(value = "/parishListByPartOf", produces = MediaType.APPLICATION_JSON_VALUE)
    public AdministrativeAreaResponseDto<List<CodeNameDto>> getParishByPartOf(
            @Parameter(description = "Query parameters: 'type' (required - parent type), 'partOfCode' (required - parent code)", required = true, example = "type=REGION&partOfCode=001")
            @RequestParam Map<String, String> queryMap) {
        return administrativeAreaService.getParishByPartOf(queryMap);
    }


    @Operation(
            summary = "Search administrative areas (full details)",
            description = "Retrieves a list of administrative areas with full details (code, name, latitude, longitude). Results are cached for 1 hour. Query parameters: 'type' (required), optionally 'partOf' for filtering by parent area."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of administrative areas with full details",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "400", description = "Missing required parameters")
    })
    @GetMapping(value = "/searchList", produces = MediaType.APPLICATION_JSON_VALUE)
    public AdministrativeAreaResponseDto<List<? extends AdministrativeAreaDto>> searchList(
            @Parameter(description = "Query parameters: 'type' (required), 'partOf' (optional)", required = true, example = "type=REGION")
            @RequestParam Map<String, String> queryMap) {
        return administrativeAreaService.searchList(queryMap);
    }



    @Operation(
            summary = "Search a single administrative area (full details)",
            description = "Retrieves a single administrative area with full details (code, name, latitude, longitude). Results are cached for 1 hour. Query parameters: 'type' (required), 'code' (required), optionally 'partOf' for hierarchical queries."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Administrative area found with full details",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Administrative area not found"),
            @ApiResponse(responseCode = "400", description = "Missing required parameters")
    })
    @GetMapping(value = "/searchOne", produces = MediaType.APPLICATION_JSON_VALUE)
    public AdministrativeAreaResponseDto<? extends AdministrativeAreaDto> searchOne(
            @Parameter(description = "Query parameters: 'type' (required), 'code' (required), 'partOf' (optional)", required = true, example = "type=REGION&code=001")
            @RequestParam Map<String, String> queryMap) {
        return administrativeAreaService.searchOne(queryMap);
    }


}

