package com.telas.shared.constants;

import org.springframework.http.HttpMethod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllowedEndpointsConstants {
    protected static final Map<HttpMethod, List<String>> ALLOWED_ENDPOINTS = new HashMap<>();

    static {
        ALLOWED_ENDPOINTS.put(HttpMethod.GET, List.of(
                "/clients/{identificationNumber}",
                "/clients/{id}",
                "/swagger-ui/**",
                "/v*/api-docs/**",
                "/actuator/**",
                "/swagger-resources/**",
                "/docs"
        ));
        ALLOWED_ENDPOINTS.put(HttpMethod.POST, List.of(
                "/clients",
                "/clients/resend-code/{identification}",
                "/auth/login",
                "/auth/recovery-password/{identificationNumber}"
        ));
        ALLOWED_ENDPOINTS.put(HttpMethod.PATCH, List.of(
                "/clients/create-password/{identification}",
                "/clients/update-contact/{identification}",
                "/clients/validate-code/{identification}",
                "/auth/reset-password/{identificationNumber}"
        ));
    }

    private AllowedEndpointsConstants() {
    }

    public static Map<HttpMethod, List<String>> getAllowedEndpoints() {
        return ALLOWED_ENDPOINTS;
    }

    public static boolean isAllowedURL(HttpMethod method, String uri) {
        String normalizedUri = uri.replaceAll("/\\d+", "/*").replaceAll("/[a-f0-9\\-]{36}", "/*").replaceAll("\\{\\w+\\}", "*");
        return ALLOWED_ENDPOINTS.getOrDefault(method, List.of()).stream()
                .anyMatch(allowedUri -> allowedUri.replaceAll("\\{\\w+\\}", "*").equals(normalizedUri));
    }
}
