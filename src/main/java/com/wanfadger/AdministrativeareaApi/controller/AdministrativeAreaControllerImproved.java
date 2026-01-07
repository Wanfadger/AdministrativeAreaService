package com.wanfadger.AdministrativeareaApi.controller;

import com.wanfadger.AdministrativeareaApi.dto.*;
import com.wanfadger.AdministrativeareaApi.dto.reponses.AdministrativeAreaResponseDto;
import com.wanfadger.AdministrativeareaApi.service.administrativearea.AdministrativeAreaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Improved Administrative Area Controller with optimized caching
 * 
 * Key improvements:
 * 1. Service-level caching with clean JSON support
 * 2. Selective cache eviction (where possible)
 * 3. Consistent cache manager usage
 * 4. Cache conditions for error handling
 * 5. Swagger/OpenAPI documentation
 * 6. JSON media type declarations
 * 
 * Migration steps:
 * 1. Review changes
 * 2. Test thoroughly
 * 3. Replace existing controller
 */
@RestController
@RequiredArgsConstructor
@CrossOrigin()
@RequestMapping("/AdministrativeAreas2")
@Tag(name = "Administrative Areas2", description = "API for managing Ugandan administrative areas (Region, Sub-Region, Local Government, County, Sub-County, Parish)")
public class AdministrativeAreaControllerImproved {

    private final AdministrativeAreaService administrativeAreaService;

    @Operation(
            summary = "Create a single administrative area",
            description = "Creates a new administrative area. Requires 'type' query parameter to specify the administrative area type. " +
                    "Valid types: REGION, SUB REGION, LOCAL GOVERNMENT, COUNTY, SUB COUNTY, PARISH. " +
                    "Example query: ?type=REGION or ?type=SUB REGION&partOf=001. " +
                    "Cache is automatically evicted after creation."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Administrative area created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AdministrativeAreaResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/one", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AdministrativeAreaResponseDto<String>> newOne(
            @Parameter(description = "Query parameters: 'type' (required: REGION|SUB REGION|LOCAL GOVERNMENT|COUNTY|SUB COUNTY|PARISH)", 
                    required = true,
                    examples = {
                        @io.swagger.v3.oas.annotations.media.ExampleObject(name = "Create Region", value = "type=REGION"),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(name = "Create Sub-Region", value = "type=SUB REGION")
                    })
            @RequestParam Map<String, String> queryMap, 
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Administrative area data", required = true)
            @RequestBody NewAdministrativeAreaDTO dto) {
        return administrativeAreaService.newOne(queryMap, dto);
    }

    @Operation(
            summary = "Create multiple administrative areas",
            description = "Creates multiple administrative areas in a single request. Requires 'type' query parameter. " +
                    "Valid types: REGION, SUB REGION, LOCAL GOVERNMENT, COUNTY, SUB COUNTY, PARISH. " +
                    "Example query: ?type=REGION or ?type=COUNTY&partOf=LG001. " +
                    "Cache is automatically evicted after creation."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Administrative areas created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AdministrativeAreaResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AdministrativeAreaResponseDto<String>> newList(
            @Parameter(description = "Query parameters: 'type' (required) - Administrative area type", required = true)
            @RequestParam Map<String, String> queryMap, 
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "List of administrative areas to create", required = true)
            @RequestBody List<NewAdministrativeAreaDTO> dtos) {
        return administrativeAreaService.newList(queryMap, dtos);
    }

    @Operation(
            summary = "Bulk upload administrative areas from Excel",
            description = "Uploads administrative areas in bulk from Excel format. Supports hierarchical data (Region → Sub-Region → Local Government → County → Sub-County → Parish). Cache is automatically evicted after upload."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Upload completed successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AdministrativeAreaResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid Excel data format"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/upload", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public AdministrativeAreaResponseDto<String> upload(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "List of administrative areas in Excel format", required = true)
            @RequestBody List<AdministrativeAreaExcelDTO> administrativeAreaExcelDtos) {
        return administrativeAreaService.upload(administrativeAreaExcelDtos);
    }

    @Operation(
            summary = "Update an administrative area",
            description = "Updates an existing administrative area. Requires 'type' query parameter and 'code' in the request body. " +
                    "Valid types: REGION, SUB REGION, LOCAL GOVERNMENT, COUNTY, SUB COUNTY, PARISH. " +
                    "Example query: ?type=REGION. " +
                    "Cache is automatically evicted after update."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Administrative area updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AdministrativeAreaResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Administrative area not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping(value = "/one", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public AdministrativeAreaResponseDto<String> updateOne(
            @Parameter(description = "Query parameters: 'type' (required) - Administrative area type", required = true)
            @RequestParam Map<String, String> queryMap, 
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated administrative area data (must include 'code')", required = true)
            @RequestBody UpdateAdministrativeAreaDTO dto) {
        return administrativeAreaService.updateOne(queryMap, dto);
    }

    @Operation(
            summary = "Get a single administrative area (code and name only)",
            description = "Retrieves a single administrative area by type and code. Returns only code and name. Results are cached for 7 days. " +
                    "Query parameters: 'type' (required), 'code' (required), optionally 'partOf' for hierarchical queries. " +
                    "Valid types: REGION, SUB REGION, LOCAL GOVERNMENT, COUNTY, SUB COUNTY, PARISH. " +
                    "Examples: ?type=REGION&code=001, ?type=SUB REGION&code=SR001&partOf=001"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Administrative area found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AdministrativeAreaResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Administrative area not found"),
            @ApiResponse(responseCode = "400", description = "Missing required parameters")
    })
    @GetMapping(value = "/filterOne", produces = MediaType.APPLICATION_JSON_VALUE)
    public AdministrativeAreaResponseDto<CodeNameDTO> filterOne(
            @Parameter(description = "Query parameters: 'type' (required), 'code' (required), 'partOf' (optional)", required = true, example = "type=REGION&code=001")
            @RequestParam Map<String, String> queryMap) {
        return administrativeAreaService.filterOne(queryMap);
    }

    @Operation(
            summary = "Get a list of administrative areas (code and name only)",
            description = "Retrieves a list of administrative areas by type. Returns only code and name. Results are cached for 7 days. " +
                    "Query parameters: 'type' (required), optionally 'partOf' for filtering by parent area. " +
                    "Valid types: REGION, SUB REGION, LOCAL GOVERNMENT, COUNTY, SUB COUNTY, PARISH. " +
                    "Examples: ?type=REGION (all regions), ?type=SUB REGION&partOf=001 (sub-regions of region 001)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of administrative areas",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AdministrativeAreaResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Missing required parameters")
    })
    @GetMapping(value = "/filterList", produces = MediaType.APPLICATION_JSON_VALUE)
    public AdministrativeAreaResponseDto<List<CodeNameDTO>> filterList(
            @Parameter(description = "Query parameters: 'type' (required), 'partOf' (optional)", required = true, example = "type=REGION")
            @RequestParam Map<String, String> queryMap) {
        return administrativeAreaService.filterList(queryMap);
    }

    @Operation(
            summary = "Get parishes by parent administrative area",
            description = "Retrieves all parishes that belong to a parent administrative area (Region, Sub-Region, Local Government, County, or Sub-County). " +
                    "Returns only code and name. Results are cached for 7 days. " +
                    "Query parameters: 'type' (required - parent type), 'partOfCode' (required - parent code). " +
                    "Valid parent types: REGION, SUB REGION, LOCAL GOVERNMENT, COUNTY, SUB COUNTY. " +
                    "Examples: ?type=REGION&partOfCode=001 (parishes in region 001), ?type=COUNTY&partOfCode=CT001 (parishes in county CT001)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of parishes",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AdministrativeAreaResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Missing required parameters")
    })
    @GetMapping(value = "/parishListByPartOf", produces = MediaType.APPLICATION_JSON_VALUE)
    public AdministrativeAreaResponseDto<List<CodeNameDTO>> getParishByPartOf(
            @Parameter(description = "Query parameters: 'type' (required - parent type: REGION|SUB REGION|LOCAL GOVERNMENT|COUNTY|SUB COUNTY), 'partOfCode' (required - parent code)", 
                    required = true,
                    examples = {
                        @io.swagger.v3.oas.annotations.media.ExampleObject(name = "Parishes by Region", value = "type=REGION&partOfCode=001"),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(name = "Parishes by County", value = "type=COUNTY&partOfCode=CT001"),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(name = "Parishes by Sub-County", value = "type=SUB COUNTY&partOfCode=SC001")
                    })
            @RequestParam Map<String, String> queryMap) {
        return administrativeAreaService.getParishByPartOf(queryMap);
    }

    @Operation(
            summary = "Search administrative areas (full details)",
            description = "Retrieves a list of administrative areas with full details (code, name, latitude, longitude). Results are cached for 1 hour. " +
                    "Query parameters: 'type' (required), optionally 'partOf' for filtering by parent area. " +
                    "Valid types: REGION, SUB REGION, LOCAL GOVERNMENT, COUNTY, SUB COUNTY, PARISH. " +
                    "Examples: ?type=REGION (all regions with coordinates), ?type=LOCAL GOVERNMENT&partOf=SR001 (local governments in sub-region SR001)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of administrative areas with full details",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AdministrativeAreaResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Missing required parameters")
    })
    @GetMapping(value = "/searchList", produces = MediaType.APPLICATION_JSON_VALUE)
    public AdministrativeAreaResponseDto<?> searchList(
            @Parameter(description = "Query parameters: 'type' (required), 'partOf' (optional)", required = true, example = "type=REGION")
            @RequestParam Map<String, String> queryMap) {
        return administrativeAreaService.searchList(queryMap);
    }

    @Operation(
            summary = "Search a single administrative area (full details)",
            description = "Retrieves a single administrative area with full details (code, name, latitude, longitude). Results are cached for 1 hour. " +
                    "Query parameters: 'type' (required), 'code' (required), optionally 'partOf' for hierarchical queries. " +
                    "Valid types: REGION, SUB REGION, LOCAL GOVERNMENT, COUNTY, SUB COUNTY, PARISH. " +
                    "Examples: ?type=REGION&code=001, ?type=PARISH&code=P001&partOf=SC001"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Administrative area found with full details",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AdministrativeAreaResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Administrative area not found"),
            @ApiResponse(responseCode = "400", description = "Missing required parameters")
    })
    @GetMapping(value = "/searchOne", produces = MediaType.APPLICATION_JSON_VALUE)
    public AdministrativeAreaResponseDto<?> searchOne(
            @Parameter(description = "Query parameters: 'type' (required), 'code' (required), 'partOf' (optional)", required = true, example = "type=REGION&code=001")
            @RequestParam Map<String, String> queryMap) {
        return administrativeAreaService.searchOne(queryMap);
    }
}
