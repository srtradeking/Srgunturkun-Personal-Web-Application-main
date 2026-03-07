package com.webapp.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    /**
     * Comma-separated list of allowed origins.
     *
     * Example (prod):
     * https://www.srgunturkun.app,https://api.srgunturkun.app
     */
    @Value("${CORS_ALLOWED_ORIGINS}")
    private List<String> allowedOrigins;

    @Value("${CORS_ALLOW_CREDENTIALS:true}")
    private boolean allowCredentials;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Explicit origins (NO wildcard when credentials are enabled)
        config.setAllowedOrigins(allowedOrigins);

        // Explicit HTTP methods
        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS"
        ));

        // Explicit request headers
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "X-CSRF-Token"
        ));

        // Expose headers if needed by frontend
        config.setExposedHeaders(List.of(
                "Authorization",
                "X-CSRF-Token"
        ));

        // Allow cookies / Authorization header
        config.setAllowCredentials(allowCredentials);

        // Cache preflight for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);
        return source;
    }
}