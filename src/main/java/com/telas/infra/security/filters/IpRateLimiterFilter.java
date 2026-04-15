package com.telas.infra.security.filters;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.telas.infra.config.RateLimitProperties;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

public class IpRateLimiterFilter extends OncePerRequestFilter {

  private final RateLimitProperties properties;
  private final LoadingCache<String, RateLimiter> limiters;

  public IpRateLimiterFilter(RateLimitProperties properties) {
    this.properties = properties;
    RateLimiterConfig config = buildSharedConfig(properties);
    this.limiters =
        Caffeine.newBuilder()
            .maximumSize(properties.getCacheMaxEntries())
            .expireAfterAccess(Duration.ofMinutes(30))
            .build(
                ip ->
                    RateLimiter.of(
                        "ip-" + ip.replace(':', '_'), config));
  }

  private static RateLimiterConfig buildSharedConfig(RateLimitProperties properties) {
    return RateLimiterConfig.custom()
        .limitForPeriod(properties.getRequestsPerPeriod())
        .limitRefreshPeriod(Duration.ofSeconds(properties.getRefreshPeriodSeconds()))
        .timeoutDuration(Duration.ZERO)
        .build();
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    if (!properties.isEnabled()) {
      filterChain.doFilter(request, response);
      return;
    }
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      filterChain.doFilter(request, response);
      return;
    }
    String uri = request.getRequestURI();
    if (uri.contains("/actuator")) {
      filterChain.doFilter(request, response);
      return;
    }
    String clientIp = resolveClientIp(request);
    RateLimiter limiter = limiters.get(clientIp);
    if (!limiter.acquirePermission()) {
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write("{\"error\":\"Too many requests\"}");
      return;
    }
    filterChain.doFilter(request, response);
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
