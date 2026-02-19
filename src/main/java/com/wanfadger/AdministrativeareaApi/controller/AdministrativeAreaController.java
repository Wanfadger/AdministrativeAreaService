package com.wanfadger.AdministrativeareaApi.controller;

import com.wanfadger.AdministrativeareaApi.dto.*;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.service.administrativearea.AdministrativeAreaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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

@RestController
@RequiredArgsConstructor
@CrossOrigin()
@RequestMapping("/api/v1/administrative-areas")
@Tag(name = "Administrative Areas", description = "API for managing Ugandan administrative areas (Region, Sub-Region, Local Government, County, Sub-County, Parish)")
public class AdministrativeAreaController {

        private final AdministrativeAreaService administrativeAreaService;

        @Operation(summary = "Create a single administrative area", description = "Creates a new administrative area. Requires 'type' query parameter to specify the administrative area type (REGION, SUB REGION, LOCAL GOVERNMENT, COUNTY, SUB COUNTY, PARISH). Cache is automatically evicted after creation.", parameters = {
                        @Parameter(name = "type", description = "Administrative area type (REGION, SUBREGION, LOCALGOVERNMENT, COUNTY, SUBCOUNTY, PARISH)", required = true, schema = @Schema(type = "string"), example = "REGION")
        })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Administrative area created successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = "{\"data\":\"Success\",\"message\":\"Administrative area created\",\"status\":true}"))),
                        @ApiResponse(responseCode = "400", description = "Invalid input data"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @PostMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
        public ResponseDTO<String> createOne(
                        @RequestParam String type,
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Administrative area data", required = true) @RequestBody NewAdministrativeAreaDTO dto) {
                return administrativeAreaService.createOne(type, dto);
        }

        @Operation(summary = "Create multiple administrative areas", description = "Creates multiple administrative areas in a single request. Requires 'type' query parameter. Cache is automatically evicted after creation.", parameters = {
                        @Parameter(name = "type", description = "Administrative area type (REGION, SUBREGION, LOCALGOVERNMENT, COUNTY, SUBCOUNTY, PARISH)", required = true, schema = @Schema(type = "string"), example = "REGION")
        })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Administrative areas created successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid input data"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
        public ResponseDTO<String> createList(
                        @RequestParam String type,
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "List of administrative areas to create", required = true) @RequestBody List<NewAdministrativeAreaDTO> dtos) {
                return administrativeAreaService.createList(type, dtos);
        }

        @Operation(summary = "Bulk upload administrative areas from Excel", description = "Uploads administrative areas in bulk from Excel format. Supports hierarchical data (Region → Sub-Region → Local Government → County → Sub-County → Parish). Cache is automatically evicted after upload.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Upload completed successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid Excel data format"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @PostMapping(value = "/upload", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
        public ResponseDTO<String> upload(
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "List of administrative areas in Excel format", required = true) @RequestBody List<AdministrativeAreaExcelDTO> administrativeAreaExcelDtos) {
                return administrativeAreaService.upload(administrativeAreaExcelDtos);
        }

        @Operation(summary = "Update an administrative area", description = "Updates an existing administrative area. Requires 'type' query parameter and 'code' in the request body. Cache is automatically evicted after update.", parameters = {
                        @Parameter(name = "type", description = "Administrative area type (REGION, SUBREGION, LOCALGOVERNMENT, COUNTY, SUBCOUNTY, PARISH)", required = true, schema = @Schema(type = "string"), example = "REGION")
        })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Administrative area updated successfully"),
                        @ApiResponse(responseCode = "404", description = "Administrative area not found"),
                        @ApiResponse(responseCode = "400", description = "Invalid input data"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @PutMapping(value = "/one", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
        public ResponseDTO<String> updateOne(
                        @Parameter(hidden = true) @RequestParam Map<String, String> queryMap,
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated administrative area data (must include 'code')", required = true) @RequestBody UpdateAdministrativeAreaDTO dto) {
                return administrativeAreaService.updateOne(queryMap, dto);
        }

        @Operation(summary = "Get a single administrative area (code and name only)", description = "Retrieves a single administrative area by type and code. Returns only code and name. Results are cached for 7 days.", parameters = {
                        @Parameter(name = "type", description = "Administrative area type", required = true, schema = @Schema(type = "string"), example = "REGION"),
                        @Parameter(name = "code", description = "Administrative area code", required = true, schema = @Schema(type = "string"), example = "001"),
                        @Parameter(name = "partOf", description = "Filter by parent code (optional)", schema = @Schema(type = "string"))
        })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Administrative area found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ResponseDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Administrative area not found"),
                        @ApiResponse(responseCode = "400", description = "Missing required parameters")
        })
        @GetMapping(value = "/filter-one", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseDTO<CodeNameDTO> filterOne(
                        @Parameter(hidden = true) @RequestParam Map<String, String> queryMap) {
                return administrativeAreaService.filterOne(queryMap);
        }

        @Operation(summary = "Get a list of administrative areas (code and name only)", description = "Retrieves a list of administrative areas by type. Returns only code and name. Results are cached for 7 days. Query parameters: 'type' (required), optionally 'partOf' for filtering by parent area.", parameters = {
                        @Parameter(name = "type", description = "Administrative area type", required = true, schema = @Schema(type = "string"), example = "REGION"),
                        @Parameter(name = "partOf", description = "Filter by parent code (optional)", schema = @Schema(type = "string"))
        })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "List of administrative areas", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ResponseDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Missing required parameters")
        })
        @GetMapping(value = "/filterList", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseDTO<List<CodeNameDTO>> filterList(
                        @Parameter(hidden = true) @RequestParam Map<String, String> queryMap) {
                return administrativeAreaService.filterList(queryMap);
        }

        @Operation(summary = "Get parishes by parent administrative area", description = "Retrieves all parishes that belong to a parent administrative area (Region, Sub-Region, Local Government, County, or Sub-County). Returns only code and name. Results are cached for 7 days.", parameters = {
                        @Parameter(name = "type", description = "Parent administrative area type", required = true, schema = @Schema(type = "string"), example = "REGION"),
                        @Parameter(name = "partOfCode", description = "Parent administrative area code", required = true, schema = @Schema(type = "string"), example = "001")
        })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "List of parishes", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ResponseDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Missing required parameters")
        })
        @GetMapping(value = "/parishListByPartOf", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseDTO<List<CodeNameDTO>> getParishByPartOf(
                        @Parameter(hidden = true) @RequestParam Map<String, String> queryMap) {
                return administrativeAreaService.getParishByPartOf(queryMap);
        }

        @Operation(summary = "Search administrative areas (full details)", description = "Retrieves a paginated list of administrative areas with full details (code, name, latitude, longitude). Results are cached for 1 hour. Supports filtering, sorting, and pagination.", parameters = {
                        @Parameter(name = "type", description = "Administrative area type (REGION, SUBREGION, LOCALGOVERNMENT, COUNTY, SUBCOUNTY, PARISH)", required = true, schema = @Schema(type = "string")),
                        @Parameter(name = "page", description = "Page number (1-based)", schema = @Schema(type = "integer", defaultValue = "1")),
                        @Parameter(name = "size", description = "Page size", schema = @Schema(type = "integer", defaultValue = "10")),
                        @Parameter(name = "sortBy", description = "Sort field", schema = @Schema(type = "string", defaultValue = "id")),
                        @Parameter(name = "sortDirection", description = "Sort direction (ASC/DESC)", schema = @Schema(type = "string", defaultValue = "ASC")),
                        @Parameter(name = "partOf", description = "Filter by parent code (optional)", schema = @Schema(type = "string"))
        }, responses = {
                        @ApiResponse(responseCode = "200", description = "List of administrative areas with full details", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PaginatedResponseDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Missing required parameters")
        })
        @GetMapping(value = "/searchList", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseDTO<?> searchList(
                        @Parameter(hidden = true) @RequestParam Map<String, String> queryMap) {
                return administrativeAreaService.searchList(queryMap);
        }

        @Operation(summary = "Search a single administrative area (full details)", description = "Retrieves a single administrative area with full details (code, name, latitude, longitude). Results are cached for 1 hour.", parameters = {
                        @Parameter(name = "type", description = "Administrative area type", required = true, schema = @Schema(type = "string"), example = "REGION"),
                        @Parameter(name = "code", description = "Administrative area code", required = true, schema = @Schema(type = "string"), example = "001"),
                        @Parameter(name = "partOf", description = "Filter by parent code (optional)", schema = @Schema(type = "string"))
        })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Administrative area found with full details", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ResponseDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Administrative area not found"),
                        @ApiResponse(responseCode = "400", description = "Missing required parameters")
        })
        @GetMapping(value = "/searchOne", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseDTO<?> searchOne(
                        @Parameter(hidden = true) @RequestParam Map<String, String> queryMap) {
                return administrativeAreaService.searchOne(queryMap);
        }

        @Operation(summary = "Advanced Search", description = """
                        Retrieves a paginated list of administrative areas based on search criteria.

                        ### Flexible Filtering
                        Supports filtering using the format `field:operator=value`.

                        **Available Operators:**
                        - `EQUALS` (default): Exact match
                        - `NOT_EQUALS`: Not equal to
                        - `CONTAINS`: Case-insensitive partial match
                        - `NOT_CONTAINS`: Case-insensitive partial non-match
                        - `GT`: Greater than
                        - `LT`: Less than
                        - `GTE`: Greater than or equal to
                        - `LTE`: Less than or equal to
                        - `IN`: Checks if value is present in a list (comma-separated)

                        **Examples:**
                        - `name:contains=Central`
                        - `code:equals=001`
                        - `region.name:contains=Western` (for SubRegions)

                        **Filterable Properties (Full Paths Supported):**
                        - `id` (Long: EQUALS, NOT_EQUALS, GT, LT, GTE, LTE, IN)
                        - `name` (String: EQUALS, NOT_EQUALS, CONTAINS, NOT_CONTAINS, IN)
                        - `code` (String: EQUALS, NOT_EQUALS, CONTAINS, NOT_CONTAINS, IN)
                        - `latitude` (Double: EQUALS, GT, LT, etc.)
                        - `longitude` (Double: EQUALS, GT, LT, etc.)
                        - `archived` (Boolean: EQUALS)
                        - **Nested Paths (e.g. for SubRegions):**
                            - `region.name`, `region.code`
                            - `subRegion.region.name` (for LocalGovernments)
                            - `localGovernment.subRegion.region.code` (for Counties)
                            - `county.localGovernment.subRegion.region.name` (for SubCounties)
                            - `subCounty.county.localGovernment.subRegion.region.code` (for Parishes)

                        Defaults to `EQUALS` if no operator is specified.
                        """, parameters = {
                        @Parameter(name = "type", description = "Administrative area type (REGION, SUBREGION, LOCALGOVERNMENT, COUNTY, SUBCOUNTY, PARISH)", required = true, schema = @Schema(type = "string")),
                        @Parameter(name = "page", description = "Page number (1-based)", schema = @Schema(type = "integer", defaultValue = "1")),
                        @Parameter(name = "size", description = "Page size (max 1000 for seeding validation)", schema = @Schema(type = "integer", defaultValue = "10")),
                        @Parameter(name = "sortBy", description = "Sort field (e.g., name, code, id)", schema = @Schema(type = "string", defaultValue = "id")),
                        @Parameter(name = "sortDirection", description = "Sort direction (ASC/DESC)", schema = @Schema(type = "string", defaultValue = "ASC"))
        }, responses = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved results", content = @Content(schema = @Schema(implementation = PaginatedResponseDTO.class)))
        })
        @GetMapping(value = "/advancedSearch", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseDTO<?> advancedSearch(
                        @Parameter(hidden = true) @RequestParam Map<String, String> queryMap) {
                return administrativeAreaService.advancedSearch(queryMap);
        }

        @Operation(summary = "Soft delete an administrative area", description = "Marks an administrative area as 'archived'. Requires 'type' and 'code' query parameters. Cache is automatically evicted after deletion.", parameters = {
                        @Parameter(name = "type", description = "Administrative area type", required = true, schema = @Schema(type = "string"), example = "REGION"),
                        @Parameter(name = "code", description = "Administrative area code", required = true, schema = @Schema(type = "string"), example = "001")
        })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Administrative area deleted successfully"),
                        @ApiResponse(responseCode = "404", description = "Administrative area not found"),
                        @ApiResponse(responseCode = "400", description = "Missing required parameters"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @DeleteMapping(value = "/one", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseDTO<String> deleteOne(
                        @Parameter(hidden = true) @RequestParam Map<String, String> queryMap) {
                return administrativeAreaService.deleteOne(queryMap);
        }

}
