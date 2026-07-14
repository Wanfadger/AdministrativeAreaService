package com.wanfadger.AdministrativeareaApi.golden;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bean validation and the hardened exception handler.
 *
 * <p>Everything asserted here surfaces through {@code detail}, which the frontend renders verbatim —
 * so an error that is technically a 400 but says nothing useful is still a bug.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:validationdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
class ValidationHardeningTest {

    private static final String BASE = "/api/v1/administrative-areas";

    @Autowired MockMvc mockMvc;

    private String createRegion(String name) throws Exception {
        String body = mockMvc.perform(post(BASE).param("type", "REGION")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"name\":\"" + name + "\"}]"))
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll("(?s).*\"data\"\\s*:\\s*\\[\\s*\"([^\"]+)\".*", "$1");
    }

    /** The whole point of NewAreaValidator: @Valid on a List body validates nothing. */
    @Test
    void blankName_inAListBody_isRejectedWithTheOffendingIndex() throws Exception {
        mockMvc.perform(post(BASE).param("type", "REGION")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"name\":\"Fine\"},{\"name\":\"  \"}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("[1].name")))
                .andExpect(jsonPath("$.detail").value(containsString("name is required")));
    }

    @Test
    void nonNumericCoordinate_isRejected() throws Exception {
        mockMvc.perform(post(BASE).param("type", "REGION")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"name\":\"Bad Coords\",\"latitude\":\"north\"}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("latitude")));
    }

    /** Zero is a REAL coordinate and must be accepted, not treated as absent. */
    @Test
    void zeroCoordinate_isAccepted() throws Exception {
        mockMvc.perform(post(BASE).param("type", "REGION")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"name\":\"Null Island\",\"latitude\":\"0\",\"longitude\":\"0\"}]"))
                .andExpect(status().isCreated());
    }

    /** A partial update must NOT trip the OnCreate @NotBlank on name. */
    @Test
    void partialUpdate_withoutName_isAllowed() throws Exception {
        String code = createRegion("Partial Update Region");
        mockMvc.perform(put(BASE + "/" + code).param("type", "REGION")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"only the description\"}"))
                .andExpect(status().isOk());
    }

    /** Validation failure on the PUT body: detail used to be the literal string "Wrong values". */
    @Test
    void updateValidationFailure_hasAUsableDetail() throws Exception {
        String code = createRegion("Detail Region");
        mockMvc.perform(put(BASE + "/" + code).param("type", "REGION")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":\"not-a-number\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("latitude")))
                .andExpect(jsonPath("$.detail").value(not(containsString("Wrong values"))));
    }
}
