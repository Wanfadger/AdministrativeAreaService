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
 * End-to-end smoke test for the new RESTful resource at /api/v1/administrative-areas,
 * driving create → search → get → update → soft-delete against an in-memory H2 DB.
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

    @Test
    void create_search_get_update_delete_region() throws Exception {
        // CREATE
        MvcResult created = mockMvc.perform(post(BASE).param("type", "REGION")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Smoke Test Region\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(true))
                .andReturn();

        JsonNode createBody = objectMapper.readTree(created.getResponse().getContentAsString());
        String code = createBody.get("data").asText();
        assertThat(code).isNotBlank();

        // SEARCH (paginated) — should include the new region
        mockMvc.perform(get(BASE + "/search").param("type", "REGION")
                        .param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[?(@.code == '" + code + "')]").exists());

        // GET by code (light)
        mockMvc.perform(get(BASE + "/" + code).param("type", "REGION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value(code))
                .andExpect(jsonPath("$.data.name").value("Smoke Test Region"));

        // UPDATE
        mockMvc.perform(put(BASE + "/" + code).param("type", "REGION")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Smoke Test Region Renamed\"}"))
                .andExpect(status().isOk());

        // DELETE (soft delete)
        mockMvc.perform(delete(BASE + "/" + code).param("type", "REGION"))
                .andExpect(status().isOk());

        // GET after delete — soft-deleted row is filtered out -> 404
        mockMvc.perform(get(BASE + "/" + code).param("type", "REGION"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_requires_type() throws Exception {
        // Missing type -> MissingDataException -> 400 ProblemDetail
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No Type\"}"))
                .andExpect(status().isBadRequest());
    }
}
