package com.wanfadger.AdministrativeareaApi.beanConfig;

import com.wanfadger.AdministrativeareaApi.web.OpenApiProperties;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Swagger/OpenAPI configuration.
 *
 * <p>Served from the API port: Swagger UI at {@code /swagger-ui.html}, the document at
 * {@code /v3/api-docs} — locally that is {@code http://localhost:4401}, and in a deployed stack it is
 * whatever origin nginx exposes, since it proxies both paths through. Both are gated by
 * {@code API-DOCS-ENABLED} (see {@code springdoc.*} in the properties file).
 *
 * <p>The advertised server list is deployment-specific and therefore configured, not hardcoded — see
 * {@link OpenApiProperties}.
 */
@Configuration
@EnableConfigurationProperties(OpenApiProperties.class)
@RequiredArgsConstructor
public class SwaggerConfig {

    private final OpenApiProperties openApi;

    @Bean
    public OpenAPI administrativeAreaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Administrative Area API")
                        .description("Spring Boot REST API for managing Ugandan administrative areas at different levels (Region, Sub-Region, Local Government, County, Sub-County, Parish)")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Administrative Area API Support")
                                .email("support@administrativearea.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(servers());
    }

    /**
     * Always the local development server, plus the deployed origin when one is configured.
     *
     * <p>A blank {@code app.openapi.server-url} drops the second entry entirely rather than emitting a
     * placeholder — see {@link OpenApiProperties#getServerUrl()}.
     */
    private List<Server> servers() {
        List<Server> servers = new ArrayList<>();
        servers.add(new Server()
                .url("http://localhost:4401")
                .description("Development Server"));

        if (StringUtils.hasText(openApi.getServerUrl())) {
            servers.add(new Server()
                    .url(openApi.getServerUrl())
                    .description(openApi.getServerDescription()));
        }
        return servers;
    }

    /**
     * Business API group — the versioned administrative-area endpoints. Named "0-…" so it sorts
     * first and is the definition shown by default when Swagger UI loads.
     */
    @Bean
    public GroupedOpenApi businessApi() {
        return GroupedOpenApi.builder()
                .group("0-AdministrativeArea-API")
                .pathsToMatch("/api/v1/**")
                .build();
    }

    /** Monitoring group — Spring Boot Actuator endpoints (requires springdoc.show-actuator=true). */
    @Bean
    public GroupedOpenApi monitoringApi() {
        return GroupedOpenApi.builder()
                .group("1-Monitoring")
                .pathsToMatch("/actuator/**")
                .build();
    }
}
