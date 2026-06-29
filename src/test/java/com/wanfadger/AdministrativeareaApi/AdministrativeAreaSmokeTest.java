package com.wanfadger.AdministrativeareaApi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end smoke test for the RESTful resource at /api/v1/administrative-areas, driving
 * list-create → search (full hierarchy) → get → update → soft-delete against an in-memory H2 DB.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestCacheConfig.class)
class AdministrativeAreaSmokeTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private static final String BASE = "/api/v1/administrative-areas";

    private String firstCode(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("data").get(0).asText();
    }

    @Test
    void create_search_get_update_delete_with_hierarchy() throws Exception {
        // CREATE region (body is always a list)
        MvcResult createdRegion = mockMvc.perform(post(BASE).param("type", "REGION")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"name\":\"Smoke Region\"}]"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(true))
                .andReturn();
        String regionCode = firstCode(createdRegion);
        assertThat(regionCode).isNotBlank();

        // CREATE sub-region under the region
        MvcResult createdSub = mockMvc.perform(post(BASE).param("type", "SUBREGION")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"name\":\"Smoke Sub\",\"partOfCode\":\"" + regionCode + "\"}]"))
                .andExpect(status().isCreated())
                .andReturn();
        String subCode = firstCode(createdSub);

        // SEARCH SUBREGION filtered by parent — result carries the FULL nested hierarchy
        mockMvc.perform(get(BASE + "/search").param("type", "SUBREGION").param("partOf", regionCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[0].code").value(subCode))
                .andExpect(jsonPath("$.data[0].region.code").value(regionCode)); // nested parent present

        // GET by code — also returns the nested hierarchy
        mockMvc.perform(get(BASE + "/" + subCode).param("type", "SUBREGION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value(subCode))
                .andExpect(jsonPath("$.data.region.code").value(regionCode));

        // UPDATE
        mockMvc.perform(put(BASE + "/" + subCode).param("type", "SUBREGION")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Smoke Sub Renamed\",\"partOfCode\":\"" + regionCode + "\"}"))
                .andExpect(status().isOk());

        // DELETE sub-region (soft delete), then it is gone
        mockMvc.perform(delete(BASE + "/" + subCode).param("type", "SUBREGION"))
                .andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/" + subCode).param("type", "SUBREGION"))
                .andExpect(status().isNotFound());

        // DELETE region now that it has no children
        mockMvc.perform(delete(BASE + "/" + regionCode).param("type", "REGION"))
                .andExpect(status().isOk());
    }

    @Test
    void create_requires_type() throws Exception {
        // Missing type -> 400 (type is mandatory; it selects the targeted entity)
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"name\":\"No Type\"}]"))
                .andExpect(status().isBadRequest());
    }
}
