package com.wanfadger.AdministrativeareaApi.golden;

import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Filtering by {@code code} — the one filter an integrating service needs most, and the one that was
 * silently broken.
 *
 * <h2>The bug this locks out</h2>
 *
 * {@code code} was listed as a FILTERABLE column <i>and</i> as a RESERVED structural param.
 * {@code AreaQueryFactory} checks RESERVED first, so every {@code code:OPERATOR} filter was dropped
 * before it reached the specification — and the caller got back the <b>entire level</b>. No 400, no
 * warning, just the wrong rows.
 *
 * <p>That failure mode is worse than an error, because the obvious use of {@code code:IN} is
 * <b>validation</b>: "do these codes exist?". With the filter silently dropped, a query for two
 * nonsense codes came back holding every parish in the country, so any caller checking
 * "did I get rows back?" concluded that nonsense codes were valid. A validator that always says yes.
 *
 * <p>Hence the assertions here are on <b>exact counts and identities</b>, never merely on
 * "some rows came back" — the broken version would have passed that.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:codefilterdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
class CodeFilterTest {

    private static final String BASE = "/api/v1/administrative-areas";

    @Autowired MockMvc mockMvc;

    /** Codes of the five REGIONs seeded below, in creation order. */
    private final List<String> regionCodes = new ArrayList<>();
    private final String tag = java.util.UUID.randomUUID().toString().substring(0, 8);

    @BeforeEach
    void seedFiveRegions() throws Exception {
        regionCodes.clear();
        for (int i = 0; i < 5; i++) {
            String body = "[{\"name\":\"CodeFilter " + tag + " " + i + "\"}]";
            String response = mockMvc.perform(post(BASE).param("type", AdministrativeAreaType.REGION.name())
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            regionCodes.add(response.replaceAll("(?s).*\"data\"\\s*:\\s*\\[\\s*\"([^\"]+)\".*", "$1"));
        }
    }

    /**
     * The headline: N codes in, exactly N rows out — in ONE request. This is what lets a consumer
     * resolve or validate a batch without N round-trips.
     */
    @Test
    void codeIn_returnsExactlyTheRequestedCodes() throws Exception {
        String csv = regionCodes.get(0) + "," + regionCodes.get(2);

        mockMvc.perform(get(BASE + "/search")
                        .param("type", "REGION")
                        .param("code:IN", csv))
                .andExpect(status().isOk())
                // The broken version returned every region here.
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    /**
     * The validation case, stated as bluntly as it deserves: codes that do not exist must return
     * NOTHING. Previously this returned every region in the table.
     */
    @Test
    void codeIn_withCodesThatDoNotExist_returnsNoRows() throws Exception {
        mockMvc.perform(get(BASE + "/search")
                        .param("type", "REGION")
                        .param("code:IN", "not-a-real-code,also-not-real"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    /** A partially-valid batch resolves to just the valid half — which is how a caller spots the gap. */
    @Test
    void codeIn_withAMixOfRealAndFakeCodes_returnsOnlyTheReal() throws Exception {
        mockMvc.perform(get(BASE + "/search")
                        .param("type", "REGION")
                        .param("code:IN", regionCodes.get(1) + ",not-a-real-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.data[0].code").value(regionCodes.get(1)));
    }

    /** IN is the batch case; EQUALS is the single case, and it was broken by the same line. */
    @Test
    void codeEquals_returnsTheOneMatchingArea() throws Exception {
        mockMvc.perform(get(BASE + "/search")
                        .param("type", "REGION")
                        .param("code:EQUALS", regionCodes.get(3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.data[0].code").value(regionCodes.get(3)));
    }

    /**
     * `code:IN` must compose with `view=flat`, since a consumer resolving a batch of codes often wants
     * the lean shape. (Core's parish enrichment is the exception — it needs the nested ancestry — but a
     * picker resolving codes to names does not.)
     */
    @Test
    void codeIn_composesWithViewFlat() throws Exception {
        mockMvc.perform(get(BASE + "/search")
                        .param("type", "REGION")
                        .param("code:IN", regionCodes.get(0) + "," + regionCodes.get(1))
                        .param("view", "flat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.data[0].partOfCode").doesNotExist());   // REGION has no parent
    }

    /**
     * Two different code filters must not collide in the cache. They are distinct queries and the
     * canonical map is the cache key, so a stale hit here would serve one caller another's rows.
     */
    @Test
    void differentCodeFilters_doNotShareACacheEntry() throws Exception {
        mockMvc.perform(get(BASE + "/search").param("type", "REGION")
                        .param("code:IN", regionCodes.get(0)))
                .andExpect(jsonPath("$.data[0].code").value(regionCodes.get(0)));

        mockMvc.perform(get(BASE + "/search").param("type", "REGION")
                        .param("code:IN", regionCodes.get(4)))
                .andExpect(jsonPath("$.data[0].code").value(regionCodes.get(4)));
    }
}
