package com.telas.shared.constants;

import org.springframework.http.HttpMethod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllowedEndpointsConstants {
    protected static final Map<HttpMethod, List<String>> ALLOWED_ENDPOINTS = new HashMap<>();

    static {
        ALLOWED_ENDPOINTS.put(HttpMethod.GET, List.of(
                "/clients/{email}",
                "/clients/{id}",
                "/addresses/{zipCode}",
                "/boxes/ads",
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
                "/clients/resend-code/{email}",
                "/webhook",
                "/auth/login",
                "/auth/recovery-password/{email}",
                "/boxes/health"
        ));
        ALLOWED_ENDPOINTS.put(HttpMethod.PATCH, List.of(
                "/clients/create-password/{email}",
                "/clients/validate-code/{email}",
                "/auth/reset-password/{email}"
        ));
    }

    private AllowedEndpointsConstants() {
    }

    public static Map<HttpMethod, List<String>> getAllowedEndpoints() {
        return ALLOWED_ENDPOINTS;
    }

    public static boolean isAllowedURL(HttpMethod method, String uri) {
        return ALLOWED_ENDPOINTS.getOrDefault(method, List.of()).stream()
                .map(allowedUri -> allowedUri.replaceAll("\\{\\w+\\}", "[^/]+"))
                .anyMatch(regex -> uri.matches("^" + regex + "$"));
    }
}
