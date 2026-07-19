package com.wanfadger.AdministrativeareaApi.web;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The deployed server Swagger UI should call ({@code app.openapi.*}).
 *
 * <h2>Why this is configuration and not a constant</h2>
 *
 * The OpenAPI document's {@code servers} list is what Swagger UI's "Try it out" actually sends its
 * requests to. It is therefore a property of <i>where this instance is deployed</i>, not of the code —
 * the same image runs on a developer's laptop, on a staging box and on the live host, and each answers
 * to a different origin. Hardcoding it meant the document advertised one fixed URL everywhere, so
 * "Try it out" was wrong in every environment except the one the constant happened to name.
 *
 * <p><b>The value to set is the origin the browser reaches, not the API's own port.</b> nginx serves
 * the console and reverse-proxies {@code /swagger-ui.html}, {@code /swagger-ui/} and
 * {@code /v3/api-docs} through to this API, so a user opening Swagger is on the console's origin. Point
 * this at that origin and "Try it out" stays same-origin and needs no CORS grant; point it at the API's
 * directly-published port instead and every call from the UI becomes cross-origin and is blocked.
 *
 * <p>Note this only affects the <i>documentation</i>. It does not bind, route or restrict anything —
 * getting it wrong makes "Try it out" fail, nothing more.
 */
@ConfigurationProperties(prefix = "app.openapi")
@Getter
@Setter
public class OpenApiProperties {

    /**
     * Public origin of the deployed API, e.g. {@code http://154.72.196.32:5002}. No trailing slash.
     *
     * <p>Blank by default, and blank means the entry is <b>omitted</b> rather than guessed. The
     * previous hardcoded {@code https://api.administrativearea.com} was a host that does not exist:
     * offering an unreachable option in Swagger's server dropdown is worse than offering none, because
     * a reader cannot tell a placeholder from a real endpoint. Set it via {@code OPENAPI-SERVER-URL}
     * in any environment that has a stable public origin; leave it blank in local development, where
     * the localhost entry below is already correct.
     */
    private String serverUrl = "";

    /** Label shown for {@link #serverUrl} in Swagger's server dropdown. Ignored when the URL is blank. */
    private String serverDescription = "Deployed Server";
}
