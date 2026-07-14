package com.wanfadger.AdministrativeareaApi.golden;

import com.wanfadger.AdministrativeareaApi.TestCacheConfig;
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
 * Defects in the implementation, written as the behaviour we WANT.
 *
 * <p>Each test is {@link Disabled} until the plan step that fixes it lands, then enabled — so it is
 * the proof the fix actually took effect. These are deliberately NOT golden files: a golden file
 * would freeze the bug into the contract.
 *
 * <p>Runs on its own database. {@link #duplicateSubCounty_shouldConflict} used to *succeed* in
 * creating a duplicate row, which would otherwise have polluted the shared golden fixture.
 *
 * <h2>Status</h2>
 * <ul>
 *   <li>✅ <b>FIXED (step 4)</b> — duplicate SUBCOUNTY create returned <b>201</b> and inserted the
 *       duplicate. {@code createOne} called {@code findByNameIgnoreCaseAndCounty_Id} with a
 *       {@code partOfCode}: a CODE passed to an ID parameter. Both are Strings, so it compiled and
 *       simply never matched. The {@code _Id} method is now deleted outright, so the call cannot be
 *       written again.</li>
 *   <li>⏳ {@code latitude=""} on create → <b>500</b> (NumberFormatException) — want 201, null</li>
 *   <li>⏳ {@code size=0} → <b>500</b> (IllegalArgumentException) — want a clamp</li>
 *   <li>⏳ {@code sortBy=bogus} → <b>500</b> (PropertyReferenceException) — want 400</li>
 *   <li>⏳ {@code sortDirection=bogus} → <b>500</b> — want a default</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestCacheConfig.class)
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
    @Disabled("Still broken — fixed by step 5: coordinate parsing")
    void blankCoordinateOnCreate_shouldBeTreatedAsAbsent() throws Exception {
        mockMvc.perform(post(BASE).param("type", "REGION")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"name\":\"Blank Coords\",\"latitude\":\"\",\"longitude\":\"\"}]"))
                .andExpect(status().isCreated());
    }

    /** Fixed by plan step 5: clamp size to [1, MAX_PAGE_SIZE] in AreaQueryFactory. */
    @Test
    @Disabled("Still broken — fixed by step 5: AreaQueryFactory clamps size")
    void zeroPageSize_shouldNotBe500() throws Exception {
        mockMvc.perform(get(BASE + "/search").param("type", "REGION").param("size", "0"))
                .andExpect(status().isOk());
    }

    /** Fixed by plan step 5/6: whitelist sortBy; unknown field → 400 with a usable detail. */
    @Test
    @Disabled("Still broken — fixed by step 5/6: sortBy whitelist")
    void unknownSortField_shouldBe400NotServerError() throws Exception {
        mockMvc.perform(get(BASE + "/search").param("type", "REGION").param("sortBy", "bogus"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    /** Fixed by plan step 5: Direction.fromOptionalString(...).orElse(ASC). */
    @Test
    @Disabled("Still broken — fixed by step 5: sort direction fallback")
    void unknownSortDirection_shouldNotBe500() throws Exception {
        mockMvc.perform(get(BASE + "/search").param("type", "REGION").param("sortDirection", "sideways"))
                .andExpect(status().isOk());
    }
}
