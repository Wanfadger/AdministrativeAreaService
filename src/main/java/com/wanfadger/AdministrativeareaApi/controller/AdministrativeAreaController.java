package com.wanfadger.AdministrativeareaApi.controller;

import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.service.AdministrativeAreaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RESTful administrative-area API following the URRMS Core path convention
 * ({@code /api/v1/{resource}} with CRUD + {@code /search}). This is a single
 * type-dispatched resource: every endpoint takes an {@code AdministrativeAreaType type}
 * (rendered as a required dropdown in Swagger — it selects the entity being targeted).
 *
 * <p>Reads return the full parent hierarchy of the selected type (e.g. a parish carries its
 * sub-county, county, local government, sub-region and region), so a single call gives the
 * client everything it needs to present the data however it likes.
 *
 * <p>Errors are RFC-7807 {@link ProblemDetail} responses: {@code 400} (missing/invalid input),
 * {@code 404} (area not found), {@code 409} (duplicate name under the same parent).
 */
@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping("/api/v1/administrative-areas")
@Tag(name = "Administrative Areas", description = "Manage the Ugandan administrative-area hierarchy (Region › Sub-Region › Local Government › County › Sub-County › Parish)")
public class AdministrativeAreaController {

    private final AdministrativeAreaService administrativeAreaService;

    /** Build the service query-map from the typed parameters (null/blank values skipped). */
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

    @Operation(summary = "Create administrative area(s)",
            description = "Create one or more areas of the same type. The body is always a list "
                    + "(a single create is a one-element list). For non-REGION types, each item must "
                    + "include 'partOfCode' (the parent area's code). Returns the generated code(s).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created — body contains the generated code(s)."),
            @ApiResponse(responseCode = "400", description = "Missing type, empty body, or missing/invalid 'partOfCode' for a non-REGION type.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "An area with the same name already exists under the parent.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseDTO<List<String>>> create(
            @Parameter(description = "Administrative area type", required = true)
            @RequestParam AdministrativeAreaType type,
            @RequestBody List<NewAdministrativeAreaDTO> dtos) {
        return new ResponseEntity<>(administrativeAreaService.create(params(type), dtos), HttpStatus.CREATED);
    }

    @Operation(summary = "Update an administrative area by code",
            description = "Updates the named fields of an existing area. Only non-blank fields are "
                    + "applied. For non-REGION types, 'partOfCode' (parent code) is required.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated."),
            @ApiResponse(responseCode = "400", description = "Missing type/code, or missing/invalid 'partOfCode'.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "No area with the given code and type.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping(value = "/{code}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<String> update(
            @Parameter(description = "Administrative area code", required = true)
            @PathVariable String code,
            @Parameter(description = "Administrative area type", required = true)
            @RequestParam AdministrativeAreaType type,
            @RequestBody NewAdministrativeAreaDTO dto) {
        return administrativeAreaService.updateOne(params(type, "code", code), dto);
    }

    @Operation(summary = "Paginated search (full hierarchy)",
            description = "Returns matching areas of the given type, each with its full parent "
                    + "hierarchy. Default page size is 1000 and default sort is by name (ascending), "
                    + "so the higher levels return as a single page. Advanced users may also append "
                    + "field:operator=value query params (operators: EQUALS, NOT_EQUALS, CONTAINS, "
                    + "NOT_CONTAINS, GT, LT, GTE, LTE, IN), e.g. &name:CONTAINS=ka.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated results; each item carries its full parent hierarchy."),
            @ApiResponse(responseCode = "400", description = "Missing or unsupported 'type'.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public PaginatedResponseDTO<AdministrativeAreaDTO> search(
            @Parameter(description = "Administrative area type", required = true)
            @RequestParam AdministrativeAreaType type,
            @Parameter(description = "Free-text search over name/code")
            @RequestParam(required = false) String search,
            @Parameter(description = "Parent area's code, one level up from 'type': for SUBREGION the "
                    + "region code, LOCALGOVERNMENT the sub-region code, COUNTY the local-government "
                    + "code, SUBCOUNTY the county code, PARISH the sub-county code. Ignored for REGION. "
                    + "Optional — omit for an unfiltered search of the type.")
            @RequestParam(required = false) String partOf,
            @Parameter(description = "Page number (1-based)")
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @Parameter(description = "Page size (default 1000)")
            @RequestParam(required = false, defaultValue = "1000") Integer size,
            @Parameter(description = "Field to sort by (default: name)")
            @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction: asc | desc (default: asc)")
            @RequestParam(required = false) String sortDirection,
            // Captures everything (incl. advanced field:operator filters); hidden from Swagger.
            @Parameter(hidden = true) @RequestParam Map<String, String> allParams) {
        allParams.put("type", type.name());
        return administrativeAreaService.search(allParams);
    }

    @Operation(summary = "Get an administrative area by code (full hierarchy)",
            description = "Returns the area with its full parent hierarchy. The 'data' field is the "
                    + "concrete per-level subtype of AdministrativeAreaDTO (RegionDTO, SubRegionDTO, …, "
                    + "ParishDTO) with every ancestor up to the region nested inside.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found — area with full parent hierarchy."),
            @ApiResponse(responseCode = "400", description = "Missing type/code, or unsupported type.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "No area with the given code and type.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping(value = "/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<AdministrativeAreaDTO> getOne(
            @Parameter(description = "Administrative area code", required = true)
            @PathVariable String code,
            @Parameter(description = "Administrative area type", required = true)
            @RequestParam AdministrativeAreaType type) {
        return administrativeAreaService.getOne(params(type, "code", code));
    }

    @Operation(summary = "Delete an administrative area by code",
            description = "Soft-deletes the area. Fails if it still has children.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Soft-deleted."),
            @ApiResponse(responseCode = "400", description = "Missing type/code, or the area still has children.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "No area with the given code and type.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping(value = "/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<String> delete(
            @Parameter(description = "Administrative area code", required = true)
            @PathVariable String code,
            @Parameter(description = "Administrative area type", required = true)
            @RequestParam AdministrativeAreaType type) {
        return administrativeAreaService.delete(params(type, "code", code));
    }
}
