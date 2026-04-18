package com.telas.services.impl;

import com.telas.monitoring.repositories.ApplicationLogEntityRepository;
import com.telas.scheduler.SchedulerJobRunContext;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class ApplicationLogRetentionScheduler {

    private final ApplicationLogEntityRepository applicationLogEntityRepository;
    private final SchedulerJobRunContext schedulerJobRunContext;

    @Value("${monitoring.log.retention.days:60}")
    private int retentionDays;

    @Scheduled(
            cron = "${monitoring.log.retention.cron:0 0 3 * * *}",
            zone = "${app.scheduler.zone:America/New_York}")
    @SchedulerLock(name = "purgeApplicationLogs", lockAtMostFor = "PT10M")
    @Transactional
    public void purgeOldLogs() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int rowsDeleted = applicationLogEntityRepository.deleteOlderThan(cutoff);
        schedulerJobRunContext.put("rowsDeleted", rowsDeleted);
    }
}
