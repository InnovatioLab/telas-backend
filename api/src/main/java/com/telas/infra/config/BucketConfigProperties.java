package com.telas.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "bucket")
public class BucketConfigProperties {
  private String endpoint;
  private String name;
  private String accessKey;
  private String secretKey;
}
