package com.telas.shared.constants;

import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllowedEndpointsConstants {
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    protected static final Map<HttpMethod, List<String>> ALLOWED_ENDPOINTS = new HashMap<>();

    static {
        ALLOWED_ENDPOINTS.put(HttpMethod.GET, List.of(
                "/monitoring/box-script/pending-command",
                "/clients/identification/**",
                "/clients/{id}",
                "/addresses/{zipCode}",
                "/boxes/ads",
                "/boxes/player-settings",
                "/monitors",
                "/swagger-ui/**",
                "/v*/api-docs/**",
                "/actuator/**",
                "/swagger-resources/**",
                "/docs",
                "/docs/**",
                "/terms_conditions",
                "/privacy-policy"
        ));
        ALLOWED_ENDPOINTS.put(HttpMethod.POST, List.of(
                "/clients",
                "/clients/resend-code/**",
                "/webhook",
                "/auth/login",
                "/auth/recovery-password/**",
                "/boxes/health",
                "/monitoring/heartbeat",
                "/monitoring/logs",
                "/monitoring/smart-plugs/ingest",
                "/monitoring/box-script/commands/ack"
        ));
        ALLOWED_ENDPOINTS.put(HttpMethod.PATCH, List.of(
                "/clients/create-password/**",
                "/clients/validate-code/**",
                "/auth/reset-password/**"
        ));
    }

    private AllowedEndpointsConstants() {
    }

    public static Map<HttpMethod, List<String>> getAllowedEndpoints() {
        return ALLOWED_ENDPOINTS;
    }

    public static boolean isAllowedURL(HttpMethod method, String uri) {
        String normalizedUri = uri.replaceAll("/\\d+", "/*").replaceAll("/[a-f0-9\\-]{36}", "/*").replaceAll("\\{\\w+}", "*");
        return ALLOWED_ENDPOINTS.getOrDefault(method, List.of()).stream()
                .anyMatch(allowedUri -> {
                    String normalizedAllowed = allowedUri.replaceAll("\\{\\w+}", "*");
                    if (normalizedAllowed.equals(normalizedUri)) return true;
                    return allowedUri.contains("/**") && PATH_MATCHER.match(allowedUri, uri);
                });
    }
}
