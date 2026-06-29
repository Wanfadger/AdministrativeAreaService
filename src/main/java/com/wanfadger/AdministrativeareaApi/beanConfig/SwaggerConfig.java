package com.wanfadger.AdministrativeareaApi.beanConfig;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI Configuration
 * 
 * Access Swagger UI at: http://localhost:8084/swagger-ui.html
 * Access API Docs at: http://localhost:8084/v3/api-docs
 */
@Configuration
public class SwaggerConfig {

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
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8084")
                                .description("Development Server"),
                        new Server()
                                .url("https://api.administrativearea.com")
                                .description("Production Server")));
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
