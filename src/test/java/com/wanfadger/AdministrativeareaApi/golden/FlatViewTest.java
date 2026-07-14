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

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code ?view=flat} — the opt-in lean payload.
 *
 * <p>The whole feature is only safe because it is <b>additive</b>: the default shape must not move by
 * a single byte, or every existing consumer breaks. Half of this class tests {@code flat}; the other
 * half tests that everything which is <i>not</i> exactly {@code flat} is indistinguishable from today.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:flatdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
class FlatViewTest {

    private static final String BASE = "/api/v1/administrative-areas";

    @Autowired MockMvc mockMvc;

    private final Map<AdministrativeAreaType, String> codes = new EnumMap<>(AdministrativeAreaType.class);
    private final String tag = java.util.UUID.randomUUID().toString().substring(0, 8);

    @BeforeEach
    void seedOneAreaPerLevel() throws Exception {
        String parent = null;
        for (AdministrativeAreaType type : AdministrativeAreaType.values()) {
            String body = parent == null
                    ? "[{\"name\":\"Flat " + type + " " + tag + "\"}]"
                    : "[{\"name\":\"Flat " + type + " " + tag + "\",\"partOfCode\":\"" + parent + "\"}]";
            String response = mockMvc.perform(post(BASE).param("type", type.name())
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            parent = response.replaceAll("(?s).*\"data\"\\s*:\\s*\\[\\s*\"([^\"]+)\".*", "$1");
            codes.put(type, parent);
        }
    }

    private String body(String url) throws Exception {
        return mockMvc.perform(get(url)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** A parish drops five levels of embedded ancestry for one field. */
    @Test
    void flatReplacesTheAncestryWithTheParentsCode() throws Exception {
        mockMvc.perform(get(BASE + "/search").param("type", "PARISH").param("view", "flat")
                        .param("search", tag))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].partOfCode")
                        .value(codes.get(AdministrativeAreaType.SUBCOUNTY)))
                .andExpect(jsonPath("$.data[0].name").exists())
                .andExpect(jsonPath("$.data[0].code").exists())
                .andExpect(jsonPath("$.data[0].subCounty").doesNotExist());
    }

    /** REGION is the root. Its parent code is null — not "", not fabricated. */
    @Test
    void regionHasNoParentCode() throws Exception {
        mockMvc.perform(get(BASE + "/search").param("type", "REGION").param("view", "flat")
                        .param("search", tag))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].partOfCode").doesNotExist())
                .andExpect(jsonPath("$.data[0].region").doesNotExist());
    }

    /** Every level, not just the deep ones. */
    @Test
    void everyNonRootLevelReportsItsImmediateParent() throws Exception {
        AdministrativeAreaType[] all = AdministrativeAreaType.values();
        for (int i = 1; i < all.length; i++) {
            mockMvc.perform(get(BASE + "/search").param("type", all[i].name())
                            .param("view", "flat").param("search", tag))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].partOfCode").value(codes.get(all[i - 1])));
        }
    }

    /**
     * The guarantee the whole design rests on. Omitting {@code view}, sending it blank, and sending
     * junk must all produce the response that shipped before this feature existed.
     */
    @Test
    void anythingThatIsNotExactlyFlat_isTheUnchangedNestedShape() throws Exception {
        String omitted = body(BASE + "/search?type=PARISH&search=" + tag);
        String blank = body(BASE + "/search?type=PARISH&search=" + tag + "&view=");
        String nested = body(BASE + "/search?type=PARISH&search=" + tag + "&view=nested");
        String junk = body(BASE + "/search?type=PARISH&search=" + tag + "&view=garbage");
        String cased = body(BASE + "/search?type=PARISH&search=" + tag + "&view=NESTED");

        assertThat(blank).isEqualTo(omitted);
        assertThat(nested).isEqualTo(omitted);
        assertThat(junk).isEqualTo(omitted);
        assertThat(cased).isEqualTo(omitted);
        assertThat(omitted).contains("\"subCounty\"").doesNotContain("partOfCode");
    }

    /** view=FLAT and view=flat are the same request and must not be cached twice. */
    @Test
    void flatIsCaseInsensitive() throws Exception {
        String lower = body(BASE + "/search?type=PARISH&search=" + tag + "&view=flat");
        String upper = body(BASE + "/search?type=PARISH&search=" + tag + "&view=FLAT");
        assertThat(upper).isEqualTo(lower).contains("partOfCode");
    }

    /** Flat changes the item shape and nothing else — paging, filters and the envelope all survive. */
    @Test
    void flatKeepsPagingFiltersAndTheEnvelope() throws Exception {
        mockMvc.perform(get(BASE + "/search")
                        .param("type", "PARISH").param("view", "flat")
                        .param("partOf", codes.get(AdministrativeAreaType.SUBCOUNTY))
                        .param("page", "1").param("size", "10")
                        .param("sortBy", "name").param("sortDirection", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.hasNext").exists())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data[0].partOfCode")
                        .value(codes.get(AdministrativeAreaType.SUBCOUNTY)));
    }

    /**
     * {@code getOne} is deliberately NOT flattened. It returns one row, so the payload argument does
     * not apply — and its whole purpose is to hand a caller the full hierarchy in a single call.
     */
    @Test
    void getOneIgnoresTheViewParam() throws Exception {
        mockMvc.perform(get(BASE + "/" + codes.get(AdministrativeAreaType.PARISH))
                        .param("type", "PARISH").param("view", "flat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subCounty").exists())
                .andExpect(jsonPath("$.data.partOfCode").doesNotExist());
    }
}
