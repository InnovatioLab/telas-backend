package com.telas.infra.config;

import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimiterConfig {
  @Bean
  public io.github.resilience4j.ratelimiter.RateLimiter customRateLimiter() {
    io.github.resilience4j.ratelimiter.RateLimiterConfig config = io.github.resilience4j.ratelimiter.RateLimiterConfig.custom()
            .limitForPeriod(1)
            .limitRefreshPeriod(Duration.ofMillis(100))
            .timeoutDuration(Duration.ofSeconds(1))
            .build();
    RateLimiterRegistry registry = RateLimiterRegistry.of(config);
    return registry.rateLimiter("customRateLimiter");
  }
}
