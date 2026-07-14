package com.wanfadger.AdministrativeareaApi.golden;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanfadger.AdministrativeareaApi.TestCacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Freezes the published API contract against the CURRENT implementation, before any refactoring.
 *
 * <p>Everything asserted here is consumed by the Angular frontend and by the services about to
 * integrate: the response envelopes, the full nested ancestry on every item, coordinates as JSON
 * strings ({@code ""} when absent), and the exact {@code detail} string of every error.
 *
 * <p>The error strings are deliberately inconsistent today and MUST be preserved verbatim — create
 * says {@code "Parish Already Exists in the sub county"} while update says {@code "Parish already
 * exists in the sub county"} (lower-case 'a'), and COUNTY/SUBCOUNTY create raise a bare
 * {@code "Invalid PartOfCode"} where the other levels append the offending code. Normalising them
 * would silently change what users see.
 *
 * <p>Runs on its OWN in-memory database so results can't be polluted by fixtures other test classes
 * create in the shared {@code aatest} instance.
 *
 * <h2>Contract changes recorded here</h2>
 * <ul>
 *   <li><b>Error bodies lost {@code "properties": null}.</b> The app declared a raw
 *       {@code new ObjectMapper()} bean, which backs off Boot's {@code JacksonAutoConfiguration}
 *       and therefore lost the {@code ProblemDetailJacksonMixin}. That emitted a spurious
 *       {@code properties} field RFC-7807 does not define. Deleting the bean removed it. Every
 *       other field — {@code detail}, {@code status}, {@code title}, {@code type},
 *       {@code instance} — is unchanged, and the frontend only reads {@code detail}.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestCacheConfig.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:goldendb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
class ApiContractGoldenTest {

    private static final String BASE = "/api/v1/administrative-areas";
    private static final String[] LEVELS =
            {"REGION", "SUBREGION", "LOCALGOVERNMENT", "COUNTY", "SUBCOUNTY", "PARISH"};

    /** Parent level of each type, mirroring the hierarchy the API dispatches over. */
    private static final Map<String, String> PARENT = Map.of(
            "SUBREGION", "REGION",
            "LOCALGOVERNMENT", "SUBREGION",
            "COUNTY", "LOCALGOVERNMENT",
            "SUBCOUNTY", "COUNTY",
            "PARISH", "SUBCOUNTY");

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CacheManager cacheManager;

    /** "TYPE/Name" -> generated code of that fixture area. */
    private final Map<String, String> codes = new LinkedHashMap<>();
    /** generated code -> stable token, so golden files never contain a random UUID. */
    private final Map<String, String> tokens = new LinkedHashMap<>();

    private static boolean seeded;

    @BeforeEach
    void setUp() throws Exception {
        // The cache is keyed on the query map; a fixture created after a search was cached would
        // otherwise be invisible to the next test. Writes evict today, but be explicit.
        cacheManager.getCacheNames().forEach(n -> cacheManager.getCache(n).clear());

        if (!seeded) {
            seedHierarchy();
            seeded = true;
        }
        // JUnit builds a fresh test instance per method, so rebuild the code/token maps each time.
        // EVERY area gets a token — covering only one per level would leave the second parish's raw
        // UUID in the golden file and make it fail on the next run.
        for (String level : LEVELS) {
            for (JsonNode item : searchAll(level)) {
                String code = item.get("code").asText();
                String name = item.get("name").asText();
                codes.put(level + "/" + name, code);
                tokens.put(code, "{{" + level + ":" + name + "}}");
            }
        }
    }

    private JsonNode searchAll(String type) throws Exception {
        String json = mockMvc.perform(get(BASE + "/search").param("type", type))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("data");
    }

    // ------------------------------------------------------------------ fixture

    private void seedHierarchy() throws Exception {
        // One area per level, chained. Coordinates on PARISH and REGION only, so the golden files
        // capture BOTH the populated case and the "" sentinel the API emits when they're absent.
        create("REGION", """
                [{"name":"Golden Region","description":"golden","latitude":"1.5","longitude":"32.5"}]""");
        create("SUBREGION", body("Golden SubRegion", freshCode("REGION"), null, null));
        create("LOCALGOVERNMENT", body("Golden LocalGovernment", freshCode("SUBREGION"), null, null));
        create("COUNTY", body("Golden County", freshCode("LOCALGOVERNMENT"), null, null));
        create("SUBCOUNTY", body("Golden SubCounty", freshCode("COUNTY"), null, null));
        create("PARISH", body("Golden Parish", freshCode("SUBCOUNTY"), "0.3476", "32.5825"));

        // A second parish under the same sub-county, so the paging/sorting/filtering goldens have
        // more than one row to order.
        create("PARISH", body("Another Parish", freshCode("SUBCOUNTY"), null, null));
    }

    /** During seeding the maps aren't built yet — read the single area of this level straight back. */
    private String freshCode(String type) throws Exception {
        return searchAll(type).get(0).get("code").asText();
    }

    private static String body(String name, String partOfCode, String lat, String lon) {
        StringBuilder sb = new StringBuilder("[{\"name\":\"").append(name).append('"');
        if (partOfCode != null) sb.append(",\"partOfCode\":\"").append(partOfCode).append('"');
        if (lat != null) sb.append(",\"latitude\":\"").append(lat).append('"');
        if (lon != null) sb.append(",\"longitude\":\"").append(lon).append('"');
        return sb.append("}]").toString();
    }

    private void create(String type, String json) throws Exception {
        mockMvc.perform(post(BASE).param("type", type)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    // ------------------------------------------------------------------ helpers

    /** The canonical "Golden &lt;Level&gt;" fixture area at this level. */
    private String codeOf(String type) {
        String key = type + "/Golden " + switch (type) {
            case "REGION" -> "Region";
            case "SUBREGION" -> "SubRegion";
            case "LOCALGOVERNMENT" -> "LocalGovernment";
            case "COUNTY" -> "County";
            case "SUBCOUNTY" -> "SubCounty";
            default -> "Parish";
        };
        String code = codes.get(key);
        if (code == null) throw new IllegalStateException("no fixture for " + key + " in " + codes.keySet());
        return code;
    }

    /** Replace every generated UUID with a stable token so goldens are deterministic. */
    private String normalize(String json) {
        String out = json;
        for (Map.Entry<String, String> e : tokens.entrySet()) {
            out = out.replace(e.getKey(), e.getValue());
        }
        return out;
    }

    private void golden(String name, MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(request).andReturn();
        String body = result.getResponse().getContentAsString();

        // Fold the HTTP status into the golden file. An error whose body is right but whose status
        // silently drifted from 409 to 500 would otherwise pass.
        var envelope = objectMapper.createObjectNode();
        envelope.put("httpStatus", result.getResponse().getStatus());
        envelope.set("body", body.isBlank()
                ? objectMapper.nullNode()
                : objectMapper.readTree(normalize(body)));

        GoldenFiles.assertMatches(name, objectMapper.writeValueAsString(envelope));
    }

    // ------------------------------------------------------------------ reads

    @Test
    void search_everyLevel_carriesFullNestedAncestry() throws Exception {
        for (String level : LEVELS) {
            golden("search_" + level, get(BASE + "/search").param("type", level).param("sortBy", "name"));
        }
    }

    @Test
    void search_filteredByParent() throws Exception {
        for (String level : LEVELS) {
            if (!PARENT.containsKey(level)) continue;
            golden("search_" + level + "_partOf",
                    get(BASE + "/search").param("type", level)
                            .param("partOf", codeOf(PARENT.get(level)))
                            .param("sortBy", "name"));
        }
    }

    @Test
    void search_advancedFilterGrammar() throws Exception {
        // The `field:OPERATOR=value` query-param keys the frontend's advanced search sends.
        golden("search_PARISH_contains",
                get(BASE + "/search").param("type", "PARISH")
                        .param("name:CONTAINS", "Golden").param("sortBy", "name"));
        golden("search_PARISH_not_equals",
                get(BASE + "/search").param("type", "PARISH")
                        .param("name:NOT_EQUALS", "Golden Parish").param("sortBy", "name"));
    }

    @Test
    void search_paginationEnvelope_is1Based() throws Exception {
        golden("search_PARISH_page1_size1",
                get(BASE + "/search").param("type", "PARISH")
                        .param("page", "1").param("size", "1")
                        .param("sortBy", "name").param("sortDirection", "asc"));
        golden("search_PARISH_page2_size1",
                get(BASE + "/search").param("type", "PARISH")
                        .param("page", "2").param("size", "1")
                        .param("sortBy", "name").param("sortDirection", "asc"));
        // The dashboard's count-only probe: size=1, reads only totalElements.
        golden("search_PARISH_countOnly",
                get(BASE + "/search").param("type", "PARISH").param("page", "1").param("size", "1"));
    }

    @Test
    void getOne_everyLevel() throws Exception {
        for (String level : LEVELS) {
            golden("getOne_" + level, get(BASE + "/" + codeOf(level)).param("type", level));
        }
    }

    // ------------------------------------------------------------------ errors
    // The `detail` strings below are rendered verbatim by the frontend. They are inconsistent
    // today (casing, presence of the offending code). Preserve them exactly.

    @Test
    void error_missingOrUnknownType() throws Exception {
        golden("err_search_missing_type", get(BASE + "/search"));
        golden("err_search_unknown_type", get(BASE + "/search").param("type", "PROVINCE"));
    }

    @Test
    void error_create_missingPartOfCode() throws Exception {
        for (String level : LEVELS) {
            if (!PARENT.containsKey(level)) continue;   // REGION has no parent
            golden("err_create_missing_partOf_" + level,
                    post(BASE).param("type", level)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[{\"name\":\"Orphan\"}]"));
        }
    }

    @Test
    void error_create_invalidPartOfCode() throws Exception {
        for (String level : LEVELS) {
            if (!PARENT.containsKey(level)) continue;
            golden("err_create_invalid_partOf_" + level,
                    post(BASE).param("type", level)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[{\"name\":\"Orphan\",\"partOfCode\":\"no-such-parent\"}]"));
        }
    }

    @Test
    void error_create_duplicate() throws Exception {
        for (String level : LEVELS) {
            // SUBCOUNTY is EXCLUDED on purpose: its duplicate check is broken today (createOne
            // passes a code to findByNameIgnoreCaseAndCounty_Id, an *id* parameter), so it returns
            // 201 and actually inserts the duplicate — which would both freeze the bug into a
            // golden file and pollute this class's shared fixture. It is covered, isolated, by
            // KnownBrokenContractTest#duplicateSubCounty_shouldConflict.
            if ("SUBCOUNTY".equals(level)) continue;

            String parentCode = PARENT.containsKey(level) ? codeOf(PARENT.get(level)) : null;
            String name = switch (level) {
                case "REGION" -> "Golden Region";
                case "SUBREGION" -> "Golden SubRegion";
                case "LOCALGOVERNMENT" -> "Golden LocalGovernment";
                case "COUNTY" -> "Golden County";
                case "SUBCOUNTY" -> "Golden SubCounty";
                default -> "Golden Parish";
            };
            golden("err_create_duplicate_" + level,
                    post(BASE).param("type", level)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(name, parentCode, null, null)));
        }
    }

    @Test
    void error_update_duplicate() throws Exception {
        // Rename "Another Parish" onto the existing "Golden Parish" under the same sub-county.
        // NOTE the detail string here differs from the CREATE duplicate by one letter's case.
        String anotherCode = codes.get("PARISH/Another Parish");
        golden("err_update_duplicate_PARISH",
                put(BASE + "/" + anotherCode).param("type", "PARISH")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Golden Parish\",\"partOfCode\":\"" + codeOf("SUBCOUNTY") + "\"}"));
    }

    @Test
    void error_notFound() throws Exception {
        for (String level : LEVELS) {
            golden("err_getOne_notfound_" + level,
                    get(BASE + "/no-such-code").param("type", level));
        }
    }

    @Test
    void error_deleteWithChildren() throws Exception {
        // Every level except PARISH refuses deletion while it still has children.
        for (String level : LEVELS) {
            if ("PARISH".equals(level)) continue;
            golden("err_delete_has_children_" + level,
                    delete(BASE + "/" + codeOf(level)).param("type", level));
        }
    }

    @Test
    void error_emptyBody() throws Exception {
        golden("err_create_empty_list",
                post(BASE).param("type", "REGION")
                        .contentType(MediaType.APPLICATION_JSON).content("[]"));
    }
}
