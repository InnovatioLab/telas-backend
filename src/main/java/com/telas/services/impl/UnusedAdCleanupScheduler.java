package com.telas.services.impl;

import com.telas.services.UnusedAdCleanupService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cleanup.ads.enabled", havingValue = "true", matchIfMissing = true)
public class UnusedAdCleanupScheduler {

    private final UnusedAdCleanupService unusedAdCleanupService;

    @Scheduled(cron = "${cleanup.ads.cron:0 0 5 * * *}", zone = "${cleanup.ads.zone:${app.scheduler.zone:America/New_York}}")
    @SchedulerLock(name = "cleanupUnusedAds", lockAtLeastFor = "PT1M", lockAtMostFor = "PT2H")
    public void runCleanup() {
        unusedAdCleanupService.deleteEligibleUnusedAds();
    }
}
