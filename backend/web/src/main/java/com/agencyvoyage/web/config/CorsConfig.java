package com.agencyvoyage.web.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Allows the frontend (a different origin) to call the API. Defaults to the Angular
 * dev server for local development; override via {@code AGENCY_VOYAGE_CORS_ALLOWED_ORIGINS}
 * (comma-separated) in any real deployment. Exposed as a {@code CorsConfigurationSource}
 * bean, not a {@code WebMvcConfigurer}: Spring Security's filter chain runs before
 * Spring MVC's own CORS handling, so it needs this bean directly to answer preflight
 * requests - {@code addCorsMappings} alone would never be reached for a rejected request.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${agency-voyage.cors.allowed-origins:http://localhost:4200}") List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
