package com.wanfadger.AdministrativeareaApi.golden;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Conditional requests: an unchanged level answers 304 without querying, mapping or serializing
 * anything.
 *
 * <p>The dangerous direction is the one to test hardest. A <b>stale 304 is worse than a stale
 * cache</b>: the client is told its copy is current, has no way to detect otherwise, and there is
 * nothing to expire. So the assertions that matter here are not "a 304 happens" but "a 304 stops
 * happening the instant anything the response depends on changes" — including a change at a level
 * <i>above</i> the one being requested, since every DTO embeds its ancestry.
 *
 * <p>Runs with {@code app.cache.l2-enabled=false}, which is the single-instance topology: there the
 * local version counter is authoritative, so ETags are issued. (With L2 enabled but Redis
 * unreachable, no ETag is issued at all — see {@link com.wanfadger.AdministrativeareaApi.cache.AreaVersionRegistry}.)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:etagdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "app.cache.l2-enabled=false"
})
class ETagTest {

    private static final String BASE = "/api/v1/administrative-areas";

    @Autowired MockMvc mockMvc;

    private String createRegion(String name) throws Exception {
        String body = mockMvc.perform(post(BASE).param("type", "REGION")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"name\":\"" + name + "\"}]"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll("(?s).*\"data\"\\s*:\\s*\\[\\s*\"([^\"]+)\".*", "$1");
    }

    private String etagOf(String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();
        return result.getResponse().getHeader(HttpHeaders.ETAG);
    }

    @Test
    void aRepeatRequestWithTheEtag_gets304AndNoBody() throws Exception {
        String etag = etagOf(BASE + "/search?type=REGION");

        MvcResult notModified = mockMvc.perform(get(BASE + "/search").param("type", "REGION")
                        .header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified())
                .andReturn();

        assertThat(notModified.getResponse().getContentAsString())
                .as("a 304 carries no body — that is the entire point")
                .isEmpty();
    }

    /** Revalidate every time, but never show data an edit has invalidated. */
    @Test
    void responsesAskTheClientToRevalidate() throws Exception {
        mockMvc.perform(get(BASE + "/search").param("type", "REGION"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache"));
    }

    /** THE assertion. A write must break the ETag, or clients keep stale data forever. */
    @Test
    void aWrite_invalidatesTheEtag() throws Exception {
        String before = etagOf(BASE + "/search?type=REGION");

        createRegion("Etag Buster " + java.util.UUID.randomUUID());

        mockMvc.perform(get(BASE + "/search").param("type", "REGION")
                        .header(HttpHeaders.IF_NONE_MATCH, before))
                .andExpect(status().isOk());   // NOT 304 — the level changed

        assertThat(etagOf(BASE + "/search?type=REGION"))
                .as("the new ETag must differ from the pre-write one")
                .isNotEqualTo(before);
    }

    /**
     * The subtle one. A parish response embeds its region, so renaming the REGION changes the
     * parish's bytes — and its ETag must change with them, even though no parish was touched.
     * A version counter that only bumped the written level would serve stale 304s here.
     */
    @Test
    void aWriteAtAnAncestorLevel_invalidatesTheDescendantsEtag() throws Exception {
        String region = createRegion("Ancestor " + java.util.UUID.randomUUID());
        String parishEtag = etagOf(BASE + "/search?type=PARISH");

        mockMvc.perform(put(BASE + "/" + region).param("type", "REGION")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ancestor Renamed " + java.util.UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(BASE + "/search").param("type", "PARISH")
                        .header(HttpHeaders.IF_NONE_MATCH, parishEtag))
                .andExpect(status().isOk());   // every parish embeds a region: its bytes moved
    }

    /** A write BELOW a level must not disturb it — the cascade is downward only. */
    @Test
    void aWriteAtADescendantLevel_leavesTheAncestorsEtagAlone() throws Exception {
        // Create the parent FIRST: that is itself a region write and legitimately moves the region
        // ETag, so the ETag under test has to be taken after it has settled.
        String region = createRegion("Untouched " + java.util.UUID.randomUUID());
        String regionEtag = etagOf(BASE + "/search?type=REGION");

        // A purely sub-region change.
        mockMvc.perform(post(BASE).param("type", "SUBREGION")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"name\":\"Child " + java.util.UUID.randomUUID()
                                + "\",\"partOfCode\":\"" + region + "\"}]"))
                .andExpect(status().isCreated());

        mockMvc.perform(get(BASE + "/search").param("type", "REGION")
                        .header(HttpHeaders.IF_NONE_MATCH, regionEtag))
                .andExpect(status().isNotModified());   // no RegionDTO embeds its sub-regions
    }

    /** Different queries are different resources and must not share an ETag. */
    @Test
    void differentQueries_haveDifferentEtags() throws Exception {
        String small = etagOf(BASE + "/search?type=REGION&size=10");
        String large = etagOf(BASE + "/search?type=REGION&size=100");
        String flat = etagOf(BASE + "/search?type=REGION&size=10&view=flat");
        String other = etagOf(BASE + "/search?type=COUNTY&size=10");

        assertThat(small).isNotEqualTo(large).isNotEqualTo(flat).isNotEqualTo(other);

        // ...and a client holding the size=10 ETag must not be told a size=100 page is unchanged.
        mockMvc.perform(get(BASE + "/search").param("type", "REGION").param("size", "100")
                        .header(HttpHeaders.IF_NONE_MATCH, small))
                .andExpect(status().isOk());
    }

    /** getOne is conditional too — this is the map's per-pin hover lookup. */
    @Test
    void getOneSupportsConditionalRequests() throws Exception {
        String code = createRegion("GetOne Etag " + java.util.UUID.randomUUID());
        String etag = etagOf(BASE + "/" + code + "?type=REGION");

        mockMvc.perform(get(BASE + "/" + code).param("type", "REGION")
                        .header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified());
    }

    /** A client that sends no validator always gets the full body. */
    @Test
    void withoutIfNoneMatch_theFullBodyIsReturned() throws Exception {
        mockMvc.perform(get(BASE + "/search").param("type", "REGION"))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.data").exists());
    }
}
