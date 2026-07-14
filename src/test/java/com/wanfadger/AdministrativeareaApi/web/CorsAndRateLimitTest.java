package com.wanfadger.AdministrativeareaApi.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The two pieces of hardening, and the ways each is usually got wrong.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:hardeningdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "app.cors.allowed-origins=https://console.example.com",
        "app.rate-limit.enabled=true",
        "app.rate-limit.capacity=5",
        "app.rate-limit.refill-period=1m",
        // Simulates running behind a trusted proxy, which is what lets these tests tell clients apart by
        // X-Forwarded-For. It is NOT the default — see RateLimitTrustsPeerAddressByDefaultTest for why
        // the default is off and what it protects against.
        "app.rate-limit.trust-forwarded-for=true"
})
class CorsAndRateLimitTest {

    private static final String BASE = "/api/v1/administrative-areas";

    @Autowired MockMvc mockMvc;

    // ------------------------------------------------------------------ CORS

    @Test
    void anAllowedOrigin_getsItsPreflightApproved() throws Exception {
        mockMvc.perform(options(BASE + "/search")
                        .header(HttpHeaders.ORIGIN, "https://console.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "https://console.example.com"));
    }

    /** The whole point. A bare @CrossOrigin would have waved this through. */
    @Test
    void anUnknownOrigin_isRefused() throws Exception {
        mockMvc.perform(options(BASE + "/search")
                        .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    /** Writes are the ones that matter, and allow-all exposed them too. */
    @Test
    void anUnknownOrigin_cannotPreflightAWrite() throws Exception {
        mockMvc.perform(options(BASE)
                        .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    /**
     * ETag must be readable cross-origin. If it is not exposed, the browser receives it but hides it
     * from JavaScript, no If-None-Match is ever sent back, and every conditional request quietly
     * degrades to a full response — with nothing anywhere reporting a problem.
     */
    @Test
    void etagIsExposedToBrowsers_orConditionalRequestsSilentlyDie() throws Exception {
        mockMvc.perform(get(BASE + "/search").param("type", "REGION")
                        .header(HttpHeaders.ORIGIN, "https://console.example.com"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "ETag"));
    }

    /**
     * A server-to-server caller sends no Origin header at all, and must be entirely unaffected by the
     * allowlist. This is the assertion that proves locking CORS down did not break the integrating
     * services this API exists to serve.
     */
    @Test
    void aRequestWithNoOrigin_isUntouched() throws Exception {
        mockMvc.perform(get(BASE + "/search").param("type", "REGION"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    // ------------------------------------------------------------------ rate limiting

    @Test
    void overTheLimit_gets429WithRetryAfterAndAProblemBody() throws Exception {
        // capacity=5 for this test.
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get(BASE + "/search").param("type", "REGION").with(from("10.1.1.1")))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get(BASE + "/search").param("type", "REGION").with(from("10.1.1.1")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("Rate limit exceeded")));
    }

    /** One noisy client must not take the service away from everyone else. */
    @Test
    void oneClientExhaustingItsBudget_doesNotAffectAnother() throws Exception {
        for (int i = 0; i < 6; i++) {
            mockMvc.perform(get(BASE + "/search").param("type", "REGION").with(from("10.2.2.2")));
        }
        mockMvc.perform(get(BASE + "/search").param("type", "REGION").with(from("10.2.2.2")))
                .andExpect(status().isTooManyRequests());

        mockMvc.perform(get(BASE + "/search").param("type", "REGION").with(from("10.3.3.3")))
                .andExpect(status().isOk());
    }

    /**
     * Prometheus scrapes every few seconds from one address. Rate-limiting it is how you lose your
     * metrics exactly when a traffic spike makes you need them.
     */
    @Test
    void actuatorIsNotRateLimited() throws Exception {
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(get("/actuator/health").with(from("10.4.4.4")))
                    .andExpect(status().isOk());
        }
    }

    /** Sets X-Forwarded-For, which is how the filter identifies a client behind the proxy. */
    private static org.springframework.test.web.servlet.request.RequestPostProcessor from(String ip) {
        return request -> {
            request.addHeader("X-Forwarded-For", ip);
            return request;
        };
    }
}
