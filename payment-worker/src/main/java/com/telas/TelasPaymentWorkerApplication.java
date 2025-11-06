package com.telas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableJpaRepositories(
        basePackages = "com.telas.repositories", // Também precisa escanear o 'core'
        repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class
)
public class TelasPaymentWorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(TelasPaymentWorkerApplication.class, args);
    }
}