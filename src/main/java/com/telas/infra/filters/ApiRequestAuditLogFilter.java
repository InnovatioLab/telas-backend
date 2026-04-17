package com.telas.infra.filters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telas.infra.security.model.AuthenticatedUser;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.services.ApplicationLogService;
import com.telas.shared.utils.SensitiveDataMasker;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class ApiRequestAuditLogFilter extends OncePerRequestFilter {

    private static final int MAX_BODY_CHARS = 16_000;

    private static final Set<String> SENSITIVE_ENDPOINT_PREFIXES = Set.of(
        "/api/auth/login",
        "/api/auth/recovery-password",
        "/api/auth/reset-password",
        "/api/clients/create-password",
        "/api/clients/reset-password"
    );

    private final AuthenticatedUserService authenticatedUserService;
    private final ApplicationLogService applicationLogService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request);
        try {
            filterChain.doFilter(wrapped, response);
        } finally {
            persistIfApplicable(wrapped, response);
        }
    }

    private void persistIfApplicable(ContentCachingRequestWrapper request, HttpServletResponse response) {
        String endpoint = request.getRequestURI();
        if (endpoint == null || endpoint.isBlank()) {
            return;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return;
        }
        if (isSensitiveEndpoint(endpoint)) {
            return;
        }

        AuthenticatedUser user;
        try {
            user = authenticatedUserService.getLoggedUser();
        } catch (RuntimeException ignored) {
            return;
        }
        if (user == null || user.isDeveloper() || user.client() == null) {
            return;
        }

        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith(MediaType.APPLICATION_JSON_VALUE)) {
            return;
        }

        String body = readBody(request);
        if (body == null || body.isBlank()) {
            return;
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("status", response.getStatus());
        metadata.put("method", request.getMethod());
        metadata.put("query", request.getQueryString());
        metadata.put("ip", resolveClientIp(request));
        metadata.put("userAgent", trimToNull(request.getHeader("User-Agent")));

        Object maskedBody = maskBody(body);
        metadata.put("requestBody", maskedBody);

        try {
            applicationLogService.persistApiRequestLog(
                request.getMethod(),
                endpoint,
                user.client().getId(),
                response.getStatus(),
                metadata
            );
        } catch (RuntimeException ignored) {
        }
    }

    private Object maskBody(String body) {
        String truncated = truncate(body, MAX_BODY_CHARS);
        try {
            JsonNode parsed = objectMapper.readTree(truncated);
            JsonNode masked = SensitiveDataMasker.maskJson(objectMapper, parsed);
            return objectMapper.convertValue(masked, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
            return truncated;
        }
    }

    private static boolean isSensitiveEndpoint(String endpoint) {
        for (String prefix : SENSITIVE_ENDPOINT_PREFIXES) {
            if (endpoint.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static String readBody(ContentCachingRequestWrapper request) {
        byte[] buf = request.getContentAsByteArray();
        if (buf == null || buf.length == 0) {
            return null;
        }
        Charset charset;
        try {
            charset = request.getCharacterEncoding() != null
                ? Charset.forName(request.getCharacterEncoding())
                : StandardCharsets.UTF_8;
        } catch (Exception ignored) {
            charset = StandardCharsets.UTF_8;
        }
        return new String(buf, charset);
    }

    private static String truncate(String s, int maxChars) {
        if (s == null) {
            return null;
        }
        return s.length() <= maxChars ? s : s.substring(0, maxChars);
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}

