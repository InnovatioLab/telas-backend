package com.telas.infra.security.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MonitoringApiKeyFilter extends OncePerRequestFilter {

    public static final String MONITORING_API_KEY_HEADER = "X-Monitoring-Key";

    @Value("${monitoring.api.key:}")
    private String monitoringApiKey;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (!requiresApiKey(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!StringUtils.hasText(monitoringApiKey)) {
            writeUnauthorized(response, "Monitoring API key is not configured");
            return;
        }
        String provided = request.getHeader(MONITORING_API_KEY_HEADER);
        if (!monitoringApiKey.equals(provided)) {
            writeUnauthorized(response, "Invalid or missing monitoring API key");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static boolean requiresApiKey(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getServletPath();
        return "/boxes/health".equals(path)
                || "/monitoring/heartbeat".equals(path)
                || "/monitoring/logs".equals(path);
    }

    private static void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"message\":\"" + escapeJson(message) + "\"}");
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
