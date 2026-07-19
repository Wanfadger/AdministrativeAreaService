package com.wanfadger.AdministrativeareaApi.web;

import com.wanfadger.AdministrativeareaApi.beanConfig.SwaggerConfig;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The OpenAPI document must advertise the origin this instance is actually deployed behind.
 *
 * <p>Why this is worth a test: the failure is invisible from the server side. If the server list is
 * wrong, the API keeps working perfectly for every integrating service — only Swagger UI's "Try it
 * out" breaks, in a browser, for whoever opens the docs, and it breaks as a CORS error that looks
 * like an API fault rather than a documentation one. Nothing in the logs says the list is wrong.
 *
 * <p>The blank case gets its own assertion because blank is the default that every environment
 * inherits until someone sets it. It must mean "say nothing", never "guess" — the URL this replaced
 * ({@code https://api.administrativearea.com}) was a host that does not resolve, and an unreachable
 * entry in the dropdown is indistinguishable from a real one to a reader.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:openapidb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "app.openapi.server-url=http://154.72.196.32:5002",
        "app.openapi.server-description=Deployed Server"
})
class OpenApiServersTest {

    @Autowired MockMvc mockMvc;

    /**
     * End to end: the property binds and springdoc actually serves it. The unit assertions below
     * exercise the branch, but only this one proves the {@code OPENAPI-SERVER-URL} → property →
     * document chain is connected — which is the part a deployment gets wrong.
     */
    @Test
    void configuredServerAppearsInTheServedDocument() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers[0].url").value("http://localhost:4401"))
                .andExpect(jsonPath("$.servers[1].url").value("http://154.72.196.32:5002"))
                .andExpect(jsonPath("$.servers[1].description").value("Deployed Server"));
    }

    /** Blank ⇒ the deployed entry is omitted, not emitted empty and not guessed. */
    @Test
    void blankUrlListsOnlyTheDevelopmentServer() {
        List<Server> servers = serversFor("");

        assertThat(servers).hasSize(1);
        assertThat(servers.getFirst().getUrl()).isEqualTo("http://localhost:4401");
    }

    /** Whitespace is not a URL. Treated as blank, or a stray space in a compose file ships a broken entry. */
    @Test
    void whitespaceOnlyUrlIsTreatedAsBlank() {
        assertThat(serversFor("   ")).hasSize(1);
    }

    /** The development server is always listed, so local Swagger works regardless of deployment config. */
    @Test
    void developmentServerSurvivesConfiguration() {
        assertThat(serversFor("http://154.72.196.32:5002"))
                .extracting(Server::getUrl)
                .containsExactly("http://localhost:4401", "http://154.72.196.32:5002");
    }

    private List<Server> serversFor(String url) {
        OpenApiProperties props = new OpenApiProperties();
        props.setServerUrl(url);
        return new SwaggerConfig(props).administrativeAreaOpenAPI().getServers();
    }
}
