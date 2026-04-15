package com.telas.infra.config;

import com.telas.infra.security.filters.IpRateLimiterFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimiterConfig {

  @Bean
  public IpRateLimiterFilter ipRateLimiterFilter(RateLimitProperties rateLimitProperties) {
    return new IpRateLimiterFilter(rateLimitProperties);
  }
}
