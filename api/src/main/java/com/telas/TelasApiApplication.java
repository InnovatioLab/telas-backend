package com.telas;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT1M")
@EnableAsync
@EnableJpaRepositories(
        basePackages = "com.telas.repositories",
        repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class
)
public class TelasApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(TelasApiApplication.class, args);
    }
}
