package com.wanfadger.AdministrativeareaApi.controller;

import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.cache.AreaVersionRegistry;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.service.AdministrativeAreaService;
import com.wanfadger.AdministrativeareaApi.service.query.AreaQueryFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

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
// No @CrossOrigin. It was a bare one — i.e. allow-all — which let any page on the internet drive this
// API, writes included, from a visitor's browser. The policy now lives in WebConfig as an allowlist.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/administrative-areas")
@Tag(name = "Administrative Areas", description = "Manage the Ugandan administrative-area hierarchy (Region › Sub-Region › Local Government › County › Sub-County › Parish)")
public class AdministrativeAreaController {

    private final AdministrativeAreaService administrativeAreaService;
    private final AreaQueryFactory queryFactory;
    private final AreaVersionRegistry versions;

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
            // Default group ONLY. The OnCreate group carries @NotBlank on name, which must not fire
            // here — an update is partial, and renaming is optional.
            @Validated @RequestBody NewAdministrativeAreaDTO dto) {
        return administrativeAreaService.updateOne(params(type, "code", code), dto);
    }

    @Operation(summary = "Paginated search",
            description = "Returns matching areas of the given type. By default each item carries its "
                    + "full parent hierarchy; pass view=flat for just the parent's code, which is "
                    + "roughly a tenth of the payload. Default page size is 50, max 5000. Advanced "
                    + "users may append field:operator=value query params (operators: EQUALS, "
                    + "NOT_EQUALS, CONTAINS, NOT_CONTAINS, GT, LT, GTE, LTE, IN, IS_NULL, "
                    + "IS_NOT_NULL), e.g. &name:CONTAINS=ka. The two null operators ignore the "
                    + "value but still need one sent, e.g. &parent:IS_NULL=true.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated results."),
            @ApiResponse(responseCode = "400", description = "Missing/unsupported 'type', or an unknown 'sortBy' field.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @ApiResponse(responseCode = "304", description = "Not modified — the client's cached copy is "
            + "still current. Send the ETag from a previous response in If-None-Match to get this.")
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaginatedResponseDTO<AdministrativeAreaDTO>> search(
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
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size (default 50, clamped to 5000)")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "Field to sort by (default: name). One of: code, createdDateTime, "
                    + "description, latitude, longitude, name, updatedDateTime.")
            @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction: asc | desc (default: asc)")
            @RequestParam(required = false) String sortDirection,
            @Parameter(description = "Response shape: omit for the full nested ancestry (default), "
                    + "or 'flat' for just the parent's code.")
            @RequestParam(required = false) String view,
            // Captures everything (incl. advanced field:operator filters); hidden from Swagger.
            @Parameter(hidden = true) @RequestParam Map<String, String> allParams,
            @Parameter(hidden = true) WebRequest webRequest) {

        // Canonicalise BEFORE the service, so the cache key is built from a whitelist rather than
        // from raw request params — otherwise any client could mint unbounded cache entries with a
        // junk param, each holding a full result page.
        Map<String, String> query = queryFactory.canonicalise(type, allParams);

        // The ETag is derived from a per-level version counter, so it is known WITHOUT running the
        // query. checkNotModified() short-circuits here: an unchanged level costs no database access,
        // no DTO mapping and no JSON serialization — where Spring's own ShallowEtagHeaderFilter would
        // have done all three and then thrown the body away.
        //
        // Null when Redis is unreachable: the version cannot be trusted, and a wrong ETag means a 304
        // the client has no way to detect. No ETag, full response, correct answer.
        String etag = versions.etagFor(type, query);
        if (etag != null && webRequest.checkNotModified(etag)) {
            return null;   // Spring writes the 304
        }

        PaginatedResponseDTO<AdministrativeAreaDTO> body = administrativeAreaService.search(query);
        return ResponseEntity.ok()
                // no-cache = "keep it, but always revalidate". Not no-store. The client re-uses its
                // copy on a 304, but never shows data an edit has invalidated — which matters because
                // the same API backs an admin console where a user edits an area and expects to see it.
                .cacheControl(CacheControl.noCache())
                .eTag(etag)
                .body(body);
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
    public ResponseEntity<ResponseDTO<AdministrativeAreaDTO>> getOne(
            @Parameter(description = "Administrative area code", required = true)
            @PathVariable String code,
            @Parameter(description = "Administrative area type", required = true)
            @RequestParam AdministrativeAreaType type,
            @Parameter(hidden = true) WebRequest webRequest) {

        Map<String, String> query = params(type, "code", code);

        // Same short-circuit as /search. This one carries the map's hover lookups, which fire once
        // per pin: a 304 there is a few hundred bytes instead of a re-fetched hierarchy.
        String etag = versions.etagFor(type, query);
        if (etag != null && webRequest.checkNotModified(etag)) {
            return null;
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .eTag(etag)
                .body(administrativeAreaService.getOne(query));
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
