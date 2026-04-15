package com.telas.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rate.limit")
@Getter
@Setter
public class RateLimitProperties {
  private boolean enabled = true;
  private int requestsPerPeriod = 120;
  private int refreshPeriodSeconds = 60;
  private long cacheMaxEntries = 50_000L;
}
