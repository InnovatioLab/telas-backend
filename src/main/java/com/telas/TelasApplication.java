package com.telas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.telas", repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class)
public class MarketingprojectApplication {
    public static void main(String[] args) {
        SpringApplication.run(MarketingprojectApplication.class, args);
    }
}
