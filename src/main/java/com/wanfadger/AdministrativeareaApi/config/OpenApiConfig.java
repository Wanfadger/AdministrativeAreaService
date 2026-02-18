package com.wanfadger.AdministrativeareaApi.config;

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
public class OpenApiConfig {

        @Bean
        public GroupedOpenApi businessApi() {
                return GroupedOpenApi.builder()
                                .group("Administrative-Area-API")
                                .pathsToMatch("/api/v1/**", "/AdministrativeAreas/**")
                                .build();
        }

        @Bean
        public GroupedOpenApi monitoringApi() {
                return GroupedOpenApi.builder()
                                .group("0-Monitoring")
                                .pathsToMatch("/actuator/**")
                                .build();
        }

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
}
