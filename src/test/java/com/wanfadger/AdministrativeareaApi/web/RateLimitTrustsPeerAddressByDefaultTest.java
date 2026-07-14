package com.wanfadger.AdministrativeareaApi.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The rate limiter must not be evadable by a header the caller writes.
 *
 * <p>Note what is NOT set here: {@code app.rate-limit.trust-forwarded-for}. That is the entire point —
 * this suite asserts the behaviour of the DEFAULT, so if someone flips the default back to true, this
 * fails rather than the limiter quietly becoming decorative.
 *
 * <p>The bug this locks out is the worst kind, because its symptom is success. With X-Forwarded-For
 * trusted on a directly-reachable API, an attacker rotates the header per request, every request is
 * keyed to a brand-new empty bucket, every request returns 200, and no metric, log or dashboard
 * anywhere reports that the defence has been switched off.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:xffdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "app.rate-limit.enabled=true",
        "app.rate-limit.capacity=5",
        "app.rate-limit.refill-period=1m"
})
class RateLimitTrustsPeerAddressByDefaultTest {

    private static final String SEARCH = "/api/v1/administrative-areas/search";

    @Autowired MockMvc mockMvc;

    /**
     * A caller inventing a fresh X-Forwarded-For on every request must still be limited. Every one of
     * these arrives from the same peer address, and the peer address is the only thing that counts.
     */
    @Test
    void rotatingForwardedFor_doesNotMintAFreshBucket() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get(SEARCH).param("type", "REGION")
                            .with(peer("198.51.100.1")).with(forwardedFor("203.0.113." + i)))
                    .andExpect(status().isOk());
        }

        // Sixth request, sixth invented address. If the header were trusted this would be 200 — a brand
        // new bucket — and the limit would mean nothing at all.
        mockMvc.perform(get(SEARCH).param("type", "REGION")
                        .with(peer("198.51.100.1")).with(forwardedFor("203.0.113.99")))
                .andExpect(status().isTooManyRequests());
    }

    /** The header is ignored, not merely deprioritised: sending none behaves identically. */
    @Test
    void withNoForwardedForHeaderAtAll_theLimitStillApplies() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get(SEARCH).param("type", "REGION").with(peer("198.51.100.2")))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get(SEARCH).param("type", "REGION").with(peer("198.51.100.2")))
                .andExpect(status().isTooManyRequests());
    }

    /**
     * Each test needs its own peer address. They share a Spring context, so they share the filter and
     * its bucket store — and now that the limiter counts the peer address rather than a header, both
     * tests would otherwise draw down MockMvc's single default 127.0.0.1 bucket and whichever ran
     * second would start already exhausted.
     */
    private static RequestPostProcessor peer(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private static RequestPostProcessor forwardedFor(String ip) {
        return request -> {
            request.addHeader("X-Forwarded-For", ip);
            return request;
        };
    }
}
