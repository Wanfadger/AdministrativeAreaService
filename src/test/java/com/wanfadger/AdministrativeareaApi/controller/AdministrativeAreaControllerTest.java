package com.wanfadger.AdministrativeareaApi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AdministrativeAreaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCreateRegion() throws Exception {
        NewAdministrativeAreaDTO dto = new NewAdministrativeAreaDTO();
        dto.setName("Test Region");
        dto.setDescription("Test Description");
        dto.setLatitude("1.1");
        dto.setLongitude("2.2");

        mockMvc.perform(post("/AdministrativeAreas/one")
                .param("type", AdministrativeAreaType.REGION.getAreaType())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.message").value("successfully created a region"));
    }

    @Test
    public void testCreateSubRegion() throws Exception {
        // First create a region
        NewAdministrativeAreaDTO regionDto = new NewAdministrativeAreaDTO();
        regionDto.setName("Parent Region");
        String regionCode = createRegion(regionDto);

        NewAdministrativeAreaDTO subRegionDto = new NewAdministrativeAreaDTO();
        subRegionDto.setName("Test SubRegion");
        subRegionDto.setPartOfCode(regionCode);

        mockMvc.perform(post("/AdministrativeAreas/one")
                .param("type", AdministrativeAreaType.SUBREGION.getAreaType())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subRegionDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").exists());
    }

    private String createRegion(NewAdministrativeAreaDTO dto) throws Exception {
        MvcResult result = mockMvc.perform(post("/AdministrativeAreas/one")
                .param("type", AdministrativeAreaType.REGION.getAreaType())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        // Assuming ResponseDTO structure: {"data": "code", "message": "..."}
        // Simple extraction for test purposes
        return objectMapper.readTree(response).get("data").asText();
    }
}
