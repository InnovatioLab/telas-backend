package com.telas.infra.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

final class SwaggerUiBasicAuthRequestMatcher implements RequestMatcher {

    @Override
    public boolean matches(HttpServletRequest request) {
        return matchesPath(pathWithoutContext(request));
    }

    private static String pathWithoutContext(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (path.isEmpty()) {
            return "/";
        }
        if (!path.startsWith("/")) {
            return "/" + path;
        }
        return path;
    }

    static boolean matchesPath(String path) {
        if (path.startsWith("/docs")) {
            return true;
        }
        if (path.startsWith("/v3/api-docs")) {
            return true;
        }
        if (path.startsWith("/swagger-ui")) {
            return true;
        }
        if (path.startsWith("/swagger-resources")) {
            return true;
        }
        if ("/swagger-ui.html".equals(path)) {
            return true;
        }
        if (path.startsWith("/webjars/swagger-ui")) {
            return true;
        }
        if (path.startsWith("/webjars/swagger-ui-dist")) {
            return true;
        }
        return false;
    }
}
