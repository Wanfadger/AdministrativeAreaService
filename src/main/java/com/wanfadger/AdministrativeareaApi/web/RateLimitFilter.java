package com.wanfadger.AdministrativeareaApi.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Per-client request limiting, so one caller cannot exhaust the connection pool for everyone else.
 *
 * <p>With virtual threads there is no web thread pool to provide backpressure any more: 10,000
 * concurrent requests all get a thread, and all arrive at a 20-connection pool at once. The cache's
 * per-key single-flight absorbs a stampede on the <i>same</i> key; this absorbs a flood across
 * <i>different</i> ones.
 *
 * <h2>Three things this gets right that a naive version does not</h2>
 *
 * <p><b>1. The bucket store is bounded.</b> A {@code ConcurrentHashMap<String, Bucket>} keyed by IP is
 * itself a memory-exhaustion vector: an attacker sending a single request from each of a million
 * spoofed source addresses grows it without limit, and the rate limiter becomes the outage. This uses
 * a size-bounded, TTL-expiring Caffeine cache. Evicting an idle attacker's bucket costs nothing; the
 * bound is what matters.
 *
 * <p><b>2. Actuator is exempt.</b> Prometheus scrapes {@code /actuator/prometheus} every few seconds
 * from one address. Rate-limiting the monitoring is how you lose your metrics precisely when a
 * traffic spike makes you need them.
 *
 * <p><b>3. It answers with a usable error.</b> {@code 429} plus {@code Retry-After} and an RFC-7807
 * body, in the same shape as every other error this API returns — because the frontend renders
 * {@code detail} verbatim, and an integrating service needs to know how long to back off rather than
 * hammering on.
 *
 * <h2>What it is not</h2>
 *
 * The limit is <b>per pod</b>, not per cluster: bucket4j's distributed backends need a shared store,
 * and doing that over Redis would put a network round-trip on every single request — to defend against
 * a scenario the pool sizing already survives. With N pods the effective ceiling is N × capacity, and
 * that is understood and accepted rather than overlooked.
 */
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties props;
    private final ObjectMapper objectMapper;
    private final Bandwidth limit;

    private final com.github.benmanes.caffeine.cache.Cache<String, Bucket> buckets;

    public RateLimitFilter(RateLimitProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        // Greedy refill: tokens trickle back continuously rather than all at once at the end of the
        // window. An interval refill would let a client spend its whole budget, wait, and spend it
        // again the instant the window ticks over — twice the intended burst, on the boundary.
        this.limit = Bandwidth.builder()
                .capacity(props.getCapacity())
                .refillGreedy(props.getCapacity(), props.getRefillPeriod())
                .build();
        this.buckets = Caffeine.newBuilder()
                .maximumSize(props.getMaxTrackedClients())
                .expireAfterAccess(props.getClientTtl())
                .build();
    }

    /**
     * Monitoring and docs are not rate-limited. Losing metrics during a spike is losing them exactly
     * when they matter.
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if (!props.isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        Bucket bucket = buckets.get(clientKey(request), key -> Bucket.builder().addLimit(limit).build());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1, Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds());
        log.warn("Rate limit exceeded for {} on {} — retry in {}s",
                clientKey(request), request.getRequestURI(), retryAfterSeconds);

        writeTooManyRequests(response, retryAfterSeconds);
    }

    private void writeTooManyRequests(HttpServletResponse response, long retryAfterSeconds)
            throws IOException {

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
        problem.setTitle("Too Many Requests");
        problem.setDetail("Rate limit exceeded (" + props.getCapacity() + " requests per "
                + props.getRefillPeriod().toSeconds() + "s). Retry in " + retryAfterSeconds + "s.");

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setHeader("X-RateLimit-Remaining", "0");
        objectMapper.writeValue(response.getOutputStream(), problem);
    }

    /**
     * Who the caller is.
     *
     * <p>{@code X-Forwarded-For} is client-supplied and trivially spoofed, so it is trusted only when
     * configured to be — i.e. only when a proxy we control is guaranteed to have overwritten it.
     * Trusting it on a directly-exposed service would let any caller rotate the header per request and
     * evade the limit completely, while the unspoofable peer address went ignored. The left-most entry
     * is the original client; the rest are the proxies it passed through.
     */
    private String clientKey(HttpServletRequest request) {
        if (props.isTrustForwardedFor()) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
