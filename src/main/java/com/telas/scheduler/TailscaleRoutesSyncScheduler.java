package com.telas.scheduler;

import com.telas.services.TailscaleRoutesSyncService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TailscaleRoutesSyncScheduler {

    private final TailscaleRoutesSyncService tailscaleRoutesSyncService;

    @Value("${monitoring.tailscale.enabled:false}")
    private boolean enabled;

    @Scheduled(
            fixedDelayString = "${monitoring.tailscale.sync-interval-ms:300000}",
            initialDelayString = "${monitoring.tailscale.sync-initial-delay-ms:120000}")
    @SchedulerLock(name = "tailscaleRoutesSync", lockAtMostFor = "PT15M", lockAtLeastFor = "PT5S")
    public void sync() {
        if (!enabled) {
            return;
        }
        tailscaleRoutesSyncService.syncSubnetRoutes();
    }
}
