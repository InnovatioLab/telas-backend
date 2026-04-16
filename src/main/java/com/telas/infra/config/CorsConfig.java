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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${front.base.url}") String frontBaseUrl,
            @Value("${cors.allowed-origins:}") String allowedOrigins,
            @Value("${cors.allowed-origin-patterns:}") String allowedOriginPatterns) {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = new ArrayList<>(collectExplicitOrigins(frontBaseUrl, allowedOrigins));

        if (StringUtils.hasText(allowedOriginPatterns)) {
            Set<String> patterns = new LinkedHashSet<>(origins);
            Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .forEach(patterns::add);
            configuration.setAllowedOriginPatterns(new ArrayList<>(patterns));
        } else {
            configuration.setAllowedOrigins(origins);
        }

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private static List<String> collectExplicitOrigins(String frontBaseUrl, String allowedOrigins) {
        Set<String> set = new LinkedHashSet<>();
        addFrontOriginVariants(frontBaseUrl, set);
        if (StringUtils.hasText(allowedOrigins)) {
            Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .forEach(set::add);
        }
        return new ArrayList<>(set);
    }

    private static void addFrontOriginVariants(String frontBaseUrl, Set<String> origins) {
        if (!StringUtils.hasText(frontBaseUrl)) {
            return;
        }
        String trimmed = frontBaseUrl.trim();
        origins.add(trimmed);
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("https://www.")) {
            origins.add("https://" + trimmed.substring(12));
        } else if (lower.startsWith("https://") && !lower.regionMatches(8, "www.", 0, 4)) {
            origins.add("https://www." + trimmed.substring(8));
        } else if (lower.startsWith("http://www.")) {
            origins.add("http://" + trimmed.substring(11));
        } else if (lower.startsWith("http://") && !lower.regionMatches(7, "www.", 0, 4)) {
            origins.add("http://www." + trimmed.substring(7));
        }
    }
}
