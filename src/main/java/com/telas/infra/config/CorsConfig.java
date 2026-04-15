package com.telas.infra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${front.base.url}") String frontBaseUrl,
            @Value("${cors.allowed-origin-patterns:}") String additionalPatterns) {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> patterns = new ArrayList<>();
        patterns.add("http://localhost:4200");
        patterns.add("http://127.0.0.1:4200");
        patterns.add("http://localhost:*");
        patterns.add("http://127.0.0.1:*");
        addFrontOriginVariants(frontBaseUrl, patterns);
        if (StringUtils.hasText(additionalPatterns)) {
            Arrays.stream(additionalPatterns.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(patterns::add);
        }
        configuration.setAllowedOriginPatterns(patterns);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private static void addFrontOriginVariants(String frontBaseUrl, List<String> patterns) {
        if (!StringUtils.hasText(frontBaseUrl)) {
            return;
        }
        String trimmed = frontBaseUrl.trim();
        patterns.add(trimmed);
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("https://www.")) {
            patterns.add("https://" + trimmed.substring(12));
        } else if (lower.startsWith("https://") && !lower.regionMatches(8, "www.", 0, 4)) {
            patterns.add("https://www." + trimmed.substring(8));
        } else if (lower.startsWith("http://www.")) {
            patterns.add("http://" + trimmed.substring(11));
        } else if (lower.startsWith("http://") && !lower.regionMatches(7, "www.", 0, 4)) {
            patterns.add("http://www." + trimmed.substring(7));
        }
    }
}
