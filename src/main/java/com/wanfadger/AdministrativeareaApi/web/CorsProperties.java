package com.wanfadger.AdministrativeareaApi.web;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Which <b>browser</b> origins may call this API ({@code app.cors.*}).
 *
 * <h2>What CORS does and does not protect</h2>
 *
 * This is worth stating plainly, because it is routinely misunderstood in both directions.
 *
 * <p><b>It does not restrict integrating services.</b> CORS is enforced by the <i>browser</i>, not by
 * the server. Another Spring service, a Python job, a scheduled import, {@code curl} — none of them
 * consult these headers, and the allowlist below is invisible to them. Locking this down therefore
 * costs the server-to-server consumers of this API exactly nothing, which is the reason it can be
 * locked down at all.
 *
 * <p><b>It does restrict web front ends.</b> The controller previously carried a bare
 * {@code @CrossOrigin}, which is allow-all: any page on the internet could drive this API — including
 * the writes — from a visitor's browser, using that visitor's network position. For an unauthenticated
 * API on a private network that is a real path in.
 *
 * <p>The containerised console does not appear in this list and does not need to: nginx serves it and
 * reverse-proxies {@code /api/} to the API, so it is <b>same-origin</b> and CORS never applies. Only
 * the Angular dev server ({@code ng serve} on :4200) is genuinely cross-origin.
 */
@ConfigurationProperties(prefix = "app.cors")
@Getter
@Setter
public class CorsProperties {

    /**
     * Exact origins allowed to call the API from a browser. Empty ⇒ none allowed by exact match,
     * which is the correct default for a service whose only front end is same-origin. Set the real
     * origins here (via {@code CORS-ORIGINS}) in any environment where a known browser origin is fixed.
     *
     * <p>Deliberately exact, not a broad pattern: a pattern like {@code https://*.example.com} also
     * matches {@code https://evil.attacker.example.com} if anyone can register a subdomain. For the one
     * case where exactness is impractical — local development, where the dev-server port varies — use
     * {@link #allowedOriginPatterns} with a <i>narrow</i> pattern instead.
     */
    private List<String> allowedOrigins = List.of();

    /**
     * Origin <b>patterns</b> allowed to call the API from a browser (Spring's {@code allowedOriginPatterns}).
     * Empty by default. Its reason for existing is development: the console and URRMS each run their own
     * {@code ng serve} and cannot both hold one port, so the console's port varies from run to run —
     * pinning an exact origin would mean editing config every time it moves. A single narrow pattern
     * ({@code http://localhost:*}) lets any localhost port through without reconfiguration.
     *
     * <p>Narrow on purpose: {@code http://localhost:*} widens only the <b>port</b>, and only on the local
     * machine — it can never match a remote host. Fully configurable via {@code CORS-ORIGIN-PATTERNS};
     * set it empty in an environment that has no cross-origin browser client (e.g. same-origin prod).
     */
    private List<String> allowedOriginPatterns = List.of();

    /** Methods the API actually exposes. Not a wildcard — the list is short and known. */
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");

    /** Request headers a browser client may send. */
    private List<String> allowedHeaders = List.of("Content-Type", "Accept", "If-None-Match");

    /**
     * Response headers a browser client may READ. {@code ETag} must be here or conditional requests
     * silently stop working cross-origin: the browser receives the header, hides it from JavaScript,
     * and no {@code If-None-Match} is ever sent back.
     */
    private List<String> exposedHeaders = List.of("ETag");

    /**
     * Whether cookies/credentials may be sent. Off: this API has no session, and turning it on would
     * make an origin misconfiguration far more dangerous (and is illegal with a wildcard origin).
     */
    private boolean allowCredentials = false;

    /** How long a browser may cache the preflight result. */
    private Duration maxAge = Duration.ofHours(1);
}
