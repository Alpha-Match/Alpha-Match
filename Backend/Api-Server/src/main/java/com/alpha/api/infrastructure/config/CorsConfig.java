package com.alpha.api.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS Configuration for Spring WebFlux
 * - Allows Frontend (localhost:3000) to access GraphQL API
 * - Enables credentials for cookie-based authentication
 * - Supports all standard HTTP methods
 */
@Configuration
public class CorsConfig {

    /**
     * CORS WebFilter Bean
     * - Applied to all endpoints including /graphql
     * - Handles preflight OPTIONS requests automatically
     *
     * @return CorsWebFilter configured for Frontend access
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow Frontend origin
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://127.0.0.1:3000"
//                ,"https://originally-expand-convention-router.trycloudflare.com"
        ));

        config.setAllowedMethods(Arrays.asList("POST", "OPTIONS"));

        // Allow all headers (including GraphQL-specific headers)
        config.setAllowedHeaders(List.of("*"));

        // Allow credentials (cookies, authorization headers)
        config.setAllowCredentials(true);

        // Cache preflight response for 1 hour (3600 seconds)
        config.setMaxAge(3600L);

        // Expose headers to Frontend (useful for pagination, etc.)
        config.setExposedHeaders(Arrays.asList(
                "Content-Type",
                "X-Total-Count",
                "X-Page-Number"
        ));

        // Apply CORS configuration to all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
