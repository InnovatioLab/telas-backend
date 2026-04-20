package com.telas.infra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class SmartPlugDiscoveryExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService smartPlugDiscoveryExecutor(
            @Value("${monitoring.kasa.discovery.max-parallel:20}") int maxParallel) {
        int n = Math.max(1, Math.min(maxParallel, 64));
        return Executors.newFixedThreadPool(n);
    }
}
