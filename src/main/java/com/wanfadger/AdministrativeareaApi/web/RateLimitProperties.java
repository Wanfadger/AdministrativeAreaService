package com.wanfadger.AdministrativeareaApi.web;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Per-client rate limiting ({@code app.rate-limit.*}).
 */
@ConfigurationProperties(prefix = "app.rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    private boolean enabled = true;

    /**
     * Sustained requests per {@link #refillPeriod} for one client.
     *
     * <p>Sized against what the service can actually do, not against a round number. A cache hit is
     * ~17ms and the pool holds 20 connections, so this is deliberately generous: the point is to stop
     * a runaway client or a scraper exhausting the pool, not to ration normal use. The console issues
     * roughly a dozen calls on load; a legitimate integrating service polling every second is nowhere
     * near this.
     */
    private long capacity = 300;

    /** Window over which {@link #capacity} tokens are restored. */
    private Duration refillPeriod = Duration.ofMinutes(1);

    /**
     * Distinct clients tracked at once. Bounded on purpose: a plain map keyed by IP is itself a
     * memory-exhaustion vector — an attacker sending one request each from many spoofed addresses
     * would grow it without limit, so the defence would become the vulnerability. Least-recently-used
     * entries are evicted, which at worst gives an idle attacker a fresh bucket.
     */
    private long maxTrackedClients = 100_000;

    /** How long an idle client's bucket is kept before it is discarded. */
    private Duration clientTtl = Duration.ofMinutes(10);

    /**
     * Trust {@code X-Forwarded-For} for the client's address.
     *
     * <p><b>Defaults to false, and the default is the safe one.</b> Enable it only where a proxy you
     * control is the <i>only</i> way to reach the API. Reachable directly — as it is whenever the API's
     * port is published alongside the proxy — trusting this header is worse than useless: it is
     * client-supplied, so a caller rotates it per request and is never limited, while the real,
     * unspoofable peer address is the one thing being ignored. The defence becomes decorative against
     * precisely the caller it exists to stop.
     *
     * <p>A wrong value here fails silently in the dangerous direction: everything still returns 200 and
     * nothing anywhere reports that the limiter has been bypassed. So it defaults to off, and a
     * deployment that wants it must say so.
     */
    private boolean trustForwardedFor = false;
}
