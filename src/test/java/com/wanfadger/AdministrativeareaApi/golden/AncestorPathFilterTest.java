package com.wanfadger.AdministrativeareaApi.golden;

import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Filtering by an ancestor at any depth, by filtering on the path that reaches it —
 * {@code type=PARISH&subCounty.county.localGovernment.subRegion.region.code:EQUALS=…}.
 *
 * <h2>What this is for</h2>
 *
 * {@code partOf} reaches exactly one level and takes one code, so "every parish in this region" had
 * to be assembled by the caller, level by level. Because there is no way to ask about many parents
 * at once, that walk fans out one request per parent: measured against the live gazetteer, 609
 * requests for the largest region, against a 300-per-minute rate limit. Every consumer that needed
 * the question was reconstructing, over HTTP, a join this service already performs on every nested
 * read.
 *
 * <h2>Why a path and not a dedicated parameter</h2>
 *
 * This replaced an {@code ancestorType}/{@code ancestorCode} pair that answered the same question.
 * The pair worked, but it could only ever match an ancestor's <b>code</b> with <b>EQUALS</b>. A
 * path is the ordinary filter syntax, so every column and every operator comes along for free —
 * {@code localGovernment.subRegion.region.name:EQUALS=Northern} needed a second feature under the
 * old design, and whatever anyone asked for next would have needed a third. Two of the cases below
 * exist only to show that; they are the reason for the change.
 *
 * <h2>Why the assertions are on exact counts</h2>
 *
 * Because the failure this feature replaces is not an exception — it is <b>the entire level
 * returned with HTTP 200</b>. A test asserting "some rows came back", or only that the status is
 * 200, passes just as happily against a filter that was dropped on the floor. So every case below
 * pins the count, and the fixture deliberately seeds a second subtree that must never appear.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ancestordb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
class AncestorPathFilterTest {

    private static final String BASE = "/api/v1/administrative-areas";

    @Autowired MockMvc mockMvc;

    private final String tag = UUID.randomUUID().toString().substring(0, 8);

    /** Two full six-level chains. Index 0 is the subtree under test; index 1 must never leak in. */
    private final List<String> regions = new ArrayList<>();
    private final List<String> parishesOfFirstRegion = new ArrayList<>();
    private String subCountyOfFirstRegion;

    @BeforeEach
    void seedTwoCompleteHierarchies() throws Exception {
        regions.clear();
        parishesOfFirstRegion.clear();

        for (int tree = 0; tree < 2; tree++) {
            String region = create(AdministrativeAreaType.REGION, "Region " + tag + tree, null);
            regions.add(region);
            String subRegion = create(AdministrativeAreaType.SUBREGION, "SubRegion " + tag + tree, region);
            String localGov = create(AdministrativeAreaType.LOCALGOVERNMENT, "LG " + tag + tree, subRegion);
            String county = create(AdministrativeAreaType.COUNTY, "County " + tag + tree, localGov);
            String subCounty = create(AdministrativeAreaType.SUBCOUNTY, "SubCounty " + tag + tree, county);

            // Three parishes in the first tree, two in the second — different counts, so a filter
            // that silently matched the wrong subtree could not coincidentally produce the right
            // number.
            int parishes = tree == 0 ? 3 : 2;
            for (int i = 0; i < parishes; i++) {
                String parish = create(AdministrativeAreaType.PARISH, "Parish " + tag + tree + i, subCounty);
                if (tree == 0) {
                    parishesOfFirstRegion.add(parish);
                }
            }
            if (tree == 0) {
                subCountyOfFirstRegion = subCounty;
            }
        }
    }

    /** The path from a PARISH up to {@code ancestor}, as a caller writes it. */
    private static String parishPathTo(String ancestor) {
        return switch (ancestor) {
            case "SUBCOUNTY" -> "subCounty.";
            case "COUNTY" -> "subCounty.county.";
            case "LOCALGOVERNMENT" -> "subCounty.county.localGovernment.";
            case "SUBREGION" -> "subCounty.county.localGovernment.subRegion.";
            case "REGION" -> "subCounty.county.localGovernment.subRegion.region.";
            default -> throw new IllegalArgumentException(ancestor);
        };
    }

    private String create(AdministrativeAreaType type, String name, String partOfCode) throws Exception {
        String body = "[{\"name\":\"" + name + "\""
                + (partOfCode == null ? "" : ",\"partOfCode\":\"" + partOfCode + "\"") + "}]";
        String response = mockMvc.perform(post(BASE).param("type", type.name())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return response.replaceAll("(?s).*\"data\"\\s*:\\s*\\[\\s*\"([^\"]+)\".*", "$1");
    }

    // ------------------------------------------------------------------ the headline

    @Test
    @DisplayName("every parish in a region, five levels down, in one request")
    void parishesUnderARegion() throws Exception {
        mockMvc.perform(get(BASE + "/search")
                        .param("type", "PARISH")
                        .param(parishPathTo("REGION") + "code:EQUALS", regions.get(0)))
                .andExpect(status().isOk())
                // 3, not 5: the other region's parishes must not appear. A dropped filter gives 5.
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.data[0].subCounty.county.localGovernment.subRegion.region.code")
                        .value(regions.get(0)));
    }

    @Test
    @DisplayName("the same question one level up, and at every intervening level")
    void everyAncestorDepthNarrowsCorrectly() throws Exception {
        for (String ancestor : List.of("SUBREGION", "LOCALGOVERNMENT", "COUNTY", "SUBCOUNTY")) {
            String ancestorCode = codeOfFirstTree(ancestor);
            mockMvc.perform(get(BASE + "/search")
                            .param("type", "PARISH")
                            .param(parishPathTo(ancestor) + "code:EQUALS", ancestorCode))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(3));
        }
    }

    /** The intermediate levels are filterable by ancestor too, not just the leaf. */
    @Test
    @DisplayName("intermediate levels filter by ancestor as well")
    void intermediateLevelsFilterByAncestor() throws Exception {
        mockMvc.perform(get(BASE + "/search")
                        .param("type", "SUBCOUNTY")
                        // A SUBCOUNTY sits three hops below a region, not five — the path is per
                        // level, which is exactly what the old single parameter hid.
                        .param("county.localGovernment.subRegion.region.code:EQUALS", regions.get(0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.data[0].code").value(subCountyOfFirstRegion));
    }

    @Test
    @DisplayName("it composes with view=flat, so the cheap shape stays available")
    void worksWithFlatView() throws Exception {
        mockMvc.perform(get(BASE + "/search")
                        .param("type", "PARISH")
                        .param(parishPathTo("REGION") + "code:EQUALS", regions.get(0))
                        .param("view", "flat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.data[0].partOfCode").value(subCountyOfFirstRegion))
                .andExpect(jsonPath("$.data[0].subCounty").doesNotExist());
    }

    @Test
    @DisplayName("it composes with paging rather than replacing it")
    void worksWithPaging() throws Exception {
        mockMvc.perform(get(BASE + "/search")
                        .param("type", "PARISH")
                        .param(parishPathTo("REGION") + "code:EQUALS", regions.get(0))
                        .param("size", "2").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    @DisplayName("an unknown ancestor code matches nothing rather than everything")
    void unknownAncestorCodeReturnsNoRows() throws Exception {
        mockMvc.perform(get(BASE + "/search")
                        .param("type", "PARISH")
                        .param(parishPathTo("REGION") + "code:EQUALS", "no-such-region"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ------------------------------------------------- what the old parameter pair could not do

    @Test
    @DisplayName("an ancestor is filterable by NAME, not only by code")
    void filtersByAncestorName() throws Exception {
        // The case that motivated dropping ancestorType/ancestorCode. Nothing new was added to
        // support it — 'name' was always a filterable column, and it is now reachable through the
        // path like every other column.
        mockMvc.perform(get(BASE + "/search")
                        .param("type", "COUNTY")
                        .param("localGovernment.subRegion.region.name:EQUALS", "Region " + tag + "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("every operator works on an ancestor, not just EQUALS")
    void anyOperatorWorksOnAnAncestor() throws Exception {
        // CONTAINS across a five-level join. The old pair was EQUALS-on-code by construction.
        mockMvc.perform(get(BASE + "/search")
                        .param("type", "PARISH")
                        .param(parishPathTo("REGION") + "name:CONTAINS", tag + "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        // IN over two ancestors at once — both regions, so both trees' parishes: 3 + 2.
        mockMvc.perform(get(BASE + "/search")
                        .param("type", "PARISH")
                        .param(parishPathTo("REGION") + "code:IN", regions.get(0) + "," + regions.get(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    // ------------------------------------------------------------------ misuse is loud

    @Test
    @DisplayName("a path this level does not have is a 400, not an empty page or a full one")
    void pathThatIsNotAnAncestorOfThisLevelIsRejected() throws Exception {
        // Downward, which is what ancestorType=PARISH on a REGION search used to be.
        mockMvc.perform(get(BASE + "/search")
                        .param("type", "REGION")
                        .param("subCounty.code:EQUALS", parishesOfFirstRegion.get(0)))
                .andExpect(status().isBadRequest());

        // A path that exists but skips a level: parishes join through subCounty, not straight to
        // county. Silently dropping this would answer with every parish in the country.
        mockMvc.perform(get(BASE + "/search")
                        .param("type", "PARISH")
                        .param("county.code:EQUALS", regions.get(0)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("a bad path is rejected even without X-Strict-Params")
    void badPathsAreLoudByDefault() throws Exception {
        // Deliberately different from a stray FLAT parameter, which is still dropped by default for
        // the sake of clients that have always sent one. Nobody sends a dotted path by accident,
        // and no client predates this feature, so there is nothing to stay compatible with.
        mockMvc.perform(get(BASE + "/search")
                        .param("type", "PARISH")
                        .param("nonsense.path.code:EQUALS", "x"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(BASE + "/search")
                        .param("type", "PARISH")
                        .param("nonsense", "x"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("the 400 names the paths this level offers, so guessing teaches the syntax")
    void theErrorTeachesTheAvailablePaths() throws Exception {
        mockMvc.perform(get(BASE + "/search")
                        .param("type", "COUNTY")
                        .param("region.name:EQUALS", "Northern"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("localGovernment.subRegion.region.<field> (REGION)")));
    }

    // ------------------------------------------------------------------ nothing else moved

    @Test
    @DisplayName("partOf still means the immediate parent only")
    void partOfIsUnchanged() throws Exception {
        // Still the one-level filter it always was: a region code on a PARISH search matches nothing.
        mockMvc.perform(get(BASE + "/search")
                        .param("type", "PARISH")
                        .param("partOf", regions.get(0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get(BASE + "/search")
                        .param("type", "PARISH")
                        .param("partOf", subCountyOfFirstRegion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @DisplayName("ancestor filtering narrows alongside the other filters, not instead of them")
    void composesWithColumnFilters() throws Exception {
        mockMvc.perform(get(BASE + "/search")
                        .param("type", "PARISH")
                        .param(parishPathTo("REGION") + "code:EQUALS", regions.get(0))
                        .param("code:IN", parishesOfFirstRegion.get(0) + "," + parishesOfFirstRegion.get(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    /** Resolves an ancestor's code by walking the first tree's nested parish. */
    private String codeOfFirstTree(String level) throws Exception {
        String json = mockMvc.perform(get(BASE + "/search")
                        .param("type", "PARISH")
                        .param("code:EQUALS", parishesOfFirstRegion.get(0)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String path = switch (level) {
            case "SUBCOUNTY" -> "subCounty";
            case "COUNTY" -> "subCounty.county";
            case "LOCALGOVERNMENT" -> "subCounty.county.localGovernment";
            case "SUBREGION" -> "subCounty.county.localGovernment.subRegion";
            default -> throw new IllegalArgumentException(level);
        };
        com.fasterxml.jackson.databind.JsonNode node =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).at("/data/0");
        for (String segment : path.split("\\.")) {
            node = node.get(segment);
        }
        return node.get("code").asText();
    }
}
