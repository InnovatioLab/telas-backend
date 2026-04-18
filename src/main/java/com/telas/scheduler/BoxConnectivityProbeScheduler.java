package com.telas.scheduler;

import com.telas.services.MonitoringConnectivityProbeSettingsService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

@Component
public class BoxConnectivityProbeScheduler {

    private final TaskScheduler connectivityProbeTaskScheduler;
    private final BoxConnectivityProbeRunner boxConnectivityProbeRunner;
    private final MonitoringConnectivityProbeSettingsService monitoringConnectivityProbeSettingsService;
    private volatile ScheduledFuture<?> scheduledFuture;

    public BoxConnectivityProbeScheduler(
            @Qualifier("connectivityProbeTaskScheduler") TaskScheduler connectivityProbeTaskScheduler,
            BoxConnectivityProbeRunner boxConnectivityProbeRunner,
            MonitoringConnectivityProbeSettingsService monitoringConnectivityProbeSettingsService) {
        this.connectivityProbeTaskScheduler = connectivityProbeTaskScheduler;
        this.boxConnectivityProbeRunner = boxConnectivityProbeRunner;
        this.monitoringConnectivityProbeSettingsService = monitoringConnectivityProbeSettingsService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        reschedule();
    }

    public synchronized void reschedule() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        long delayMs = monitoringConnectivityProbeSettingsService.getIntervalMs();
        scheduledFuture =
                connectivityProbeTaskScheduler.scheduleWithFixedDelay(
                        boxConnectivityProbeRunner::runLocked,
                        Instant.now(),
                        Duration.ofMillis(delayMs));
    }
}
