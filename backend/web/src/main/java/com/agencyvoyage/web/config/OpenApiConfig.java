package com.agencyvoyage.web.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API docs at /v3/api-docs, interactive UI at /swagger-ui.html - both public (see
 * SecurityConfig). Registers a "bearerAuth" scheme so the UI's Authorize button can
 * attach a JWT to requests against the endpoints that require one.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI agencyVoyageOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Agency Voyage API")
                        .description("Group-travel booking platform - trips, group bookings, hotels, reviews, auth.")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(
                                "bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
