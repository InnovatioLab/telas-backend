package com.telas.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig {
    @Bean
    public CacheControl oneDayCacheControl() {
        return CacheControl.maxAge(24, TimeUnit.HOURS);
    }
}
