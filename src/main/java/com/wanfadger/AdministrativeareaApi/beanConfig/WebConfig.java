package com.wanfadger.AdministrativeareaApi.beanConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanfadger.AdministrativeareaApi.web.CorsProperties;
import com.wanfadger.AdministrativeareaApi.web.RateLimitFilter;
import com.wanfadger.AdministrativeareaApi.web.RateLimitProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Central CORS policy, replacing the allow-all {@code @CrossOrigin} that was on the controller.
 *
 * <p>Configured once, here, rather than per-controller: an annotation on one class is an annotation
 * somebody forgets on the next one, and "which origins may call us" is a property of the service, not
 * of a handler method.
 *
 * <p>See {@link CorsProperties} for what this does and does not protect — in short, it constrains
 * browsers and is invisible to the services integrating with this API.
 */
@Configuration
@EnableConfigurationProperties({CorsProperties.class, RateLimitProperties.class})
@RequiredArgsConstructor
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    private final CorsProperties cors;

    /**
     * Rate limiting, registered ahead of everything else.
     *
     * <p>Ordering is the point: a request that is going to be rejected should be rejected before it
     * has cost anything. Sitting behind the dispatcher would mean argument binding, canonicalisation
     * and cache lookups all happening for a request that is then thrown away — which makes the flood
     * cheaper to mount than to absorb.
     */
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(RateLimitProperties props,
                                                                   ObjectMapper objectMapper) {
        FilterRegistrationBean<RateLimitFilter> registration =
                new FilterRegistrationBean<>(new RateLimitFilter(props, objectMapper));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        log.info("Rate limiting {}: {} requests per {}s per client",
                props.isEnabled() ? "enabled" : "DISABLED",
                props.getCapacity(), props.getRefillPeriod().toSeconds());
        return registration;
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        if (cors.getAllowedOrigins().isEmpty()) {
            log.info("CORS: no browser origins allowed (app.cors.allowed-origins is empty). "
                    + "Server-to-server consumers are unaffected — CORS is a browser mechanism.");
            return;
        }

        log.info("CORS: allowing browser origins {}", cors.getAllowedOrigins());
        registry.addMapping("/api/**")
                .allowedOrigins(cors.getAllowedOrigins().toArray(String[]::new))
                .allowedMethods(cors.getAllowedMethods().toArray(String[]::new))
                .allowedHeaders(cors.getAllowedHeaders().toArray(String[]::new))
                // Without ETag here, a cross-origin client never sees the header, never sends
                // If-None-Match, and every conditional request silently degrades to a full response.
                .exposedHeaders(cors.getExposedHeaders().toArray(String[]::new))
                .allowCredentials(cors.isAllowCredentials())
                .maxAge(cors.getMaxAge().toSeconds());
    }
}
