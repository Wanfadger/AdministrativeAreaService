package com.wanfadger.AdministrativeareaApi.golden;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression tests for five defects that were live in the original implementation.
 *
 * <p>Each was written first as the behaviour we WANTED and kept {@code @Disabled} — failing — until
 * the step that fixed it landed, then enabled. That is what makes them proof the fix took effect,
 * rather than a test written to match whatever the code happened to do. They are deliberately NOT
 * golden files: a golden file would have frozen the bug into the contract.
 *
 * <p>All five now pass. What they were:
 *
 * <ul>
 *   <li><b>Duplicate SUBCOUNTY create returned 201 and inserted the duplicate.</b> {@code createOne}
 *       called {@code findByNameIgnoreCaseAndCounty_Id} with a {@code partOfCode} — a CODE passed to
 *       an ID parameter. Both are Strings, so it compiled and simply never matched. Update used the
 *       correct method, which is why only create was affected. The {@code _Id} method is now deleted
 *       outright, so the call cannot be written again.</li>
 *   <li><b>{@code latitude=""} on create was a 500</b> (NumberFormatException from
 *       {@code Double.valueOf("")}) on five of the six levels. The frontend sends {@code ""} whenever
 *       the coordinate fields are left blank, so this fired on an ordinary user action. Blank now
 *       means absent, and absent means null — never 0, which is a real coordinate.</li>
 *   <li><b>{@code size=0} was a 500</b> ({@code PageRequest.of(_, 0)} throws). Now clamped.</li>
 *   <li><b>{@code sortBy=bogus} was a 500</b> (PropertyReferenceException, thrown deep in the query
 *       layer). Now a 400 naming the sortable fields.</li>
 *   <li><b>{@code sortDirection=bogus} was a 500</b>. Now falls back to ascending.</li>
 * </ul>
 *
 * <p>Runs on its own database: the sub-county case used to genuinely insert a row, which would
 * otherwise have polluted the shared golden fixture.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:brokendb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
class KnownBrokenContractTest {

    private static final String BASE = "/api/v1/administrative-areas";

    @Autowired MockMvc mockMvc;

    /** Creates REGION → … → SUBCOUNTY and returns the sub-county's parent (county) code. */
    private String seedToCounty() throws Exception {
        String region = create("REGION", "{\"name\":\"B Region\"}");
        String subRegion = create("SUBREGION", "{\"name\":\"B SubRegion\",\"partOfCode\":\"" + region + "\"}");
        String lg = create("LOCALGOVERNMENT", "{\"name\":\"B LG\",\"partOfCode\":\"" + subRegion + "\"}");
        return create("COUNTY", "{\"name\":\"B County\",\"partOfCode\":\"" + lg + "\"}");
    }

    private String create(String type, String obj) throws Exception {
        String body = mockMvc.perform(post(BASE).param("type", type)
                        .contentType(MediaType.APPLICATION_JSON).content("[" + obj + "]"))
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll("(?s).*\"data\"\\s*:\\s*\\[\\s*\"([^\"]+)\".*", "$1");
    }

    /**
     * Fixed by plan step 4/5: delete {@code findByNameIgnoreCaseAndCounty_Id} so the code-as-id
     * call cannot compile, and route every level through one generic duplicate check.
     */
    @Test
    void duplicateSubCounty_shouldConflict() throws Exception {
        String county = seedToCounty();
        String payload = "[{\"name\":\"Dup SubCounty\",\"partOfCode\":\"" + county + "\"}]";

        mockMvc.perform(post(BASE).param("type", "SUBCOUNTY")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated());

        // Today this returns 201 and inserts a second identical sub-county.
        mockMvc.perform(post(BASE).param("type", "SUBCOUNTY")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Sub County Already Exists in the county"));
    }

    /**
     * Fixed by plan step 5: parse coordinates with {@code notNullEmpty}, treating "" as absent.
     * The frontend sends {@code ""} whenever the coordinate fields are left blank, so this is a
     * 500 on an ordinary user action today — on 5 of the 6 levels (SUBREGION happens to be right).
     */
    @Test
    void blankCoordinateOnCreate_shouldBeTreatedAsAbsent() throws Exception {
        mockMvc.perform(post(BASE).param("type", "REGION")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"name\":\"Blank Coords\",\"latitude\":\"\",\"longitude\":\"\"}]"))
                .andExpect(status().isCreated());
    }

    /** Fixed by plan step 5: clamp size to [1, MAX_PAGE_SIZE] in AreaQueryFactory. */
    @Test
    void zeroPageSize_shouldNotBe500() throws Exception {
        mockMvc.perform(get(BASE + "/search").param("type", "REGION").param("size", "0"))
                .andExpect(status().isOk());
    }

    /** Fixed by plan step 5/6: whitelist sortBy; unknown field → 400 with a usable detail. */
    @Test
    void unknownSortField_shouldBe400NotServerError() throws Exception {
        mockMvc.perform(get(BASE + "/search").param("type", "REGION").param("sortBy", "bogus"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    /** Fixed by plan step 5: Direction.fromOptionalString(...).orElse(ASC). */
    @Test
    void unknownSortDirection_shouldNotBe500() throws Exception {
        mockMvc.perform(get(BASE + "/search").param("type", "REGION").param("sortDirection", "sideways"))
                .andExpect(status().isOk());
    }
}
