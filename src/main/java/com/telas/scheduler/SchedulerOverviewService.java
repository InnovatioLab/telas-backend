package com.telas.scheduler;

import com.telas.dtos.response.SchedulerJobStatusResponseDto;
import com.telas.scheduler.model.SchedulerJobRunEntity;
import com.telas.scheduler.model.SchedulerJobRunStatus;
import com.telas.scheduler.repository.SchedulerJobRunRepository;
import com.telas.services.MonitoringConnectivityProbeSettingsService;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SchedulerOverviewService {

    private final SchedulerJobRunRepository schedulerJobRunRepository;
    private final MonitoringConnectivityProbeSettingsService monitoringConnectivityProbeSettingsService;

    @Value("${monitoring.log.retention.cron:0 0 3 * * *}")
    private String monitoringLogRetentionCron;

    @Value("${monitoring.worker.heartbeat-check-interval-ms:10000}")
    private long monitoringWorkerIntervalMs;

    @Value("${subscription.cron.remove-expired-ads:0 0 4 * * *}")
    private String removeExpiredAdsCron;

    @Value("${subscription.cron.expiry-emails:0 0 6 * * *}")
    private String expiryEmailsCron;

    @Value("${cleanup.ads.cron:0 0 5 * * *}")
    private String cleanupAdsCron;

    @Value("${app.scheduler.zone:America/New_York}")
    private String appSchedulerZone;

    @Transactional(readOnly = true)
    public List<SchedulerJobStatusResponseDto> listJobStatus() {
        long boxConnectivityProbeIntervalMs = monitoringConnectivityProbeSettingsService.getIntervalMs();
        ZoneId zoneId = ZoneId.of(appSchedulerZone);
        List<SchedulerJobStatusResponseDto> out = new ArrayList<>();
        out.add(
                buildForCron(
                        "purgeApplicationLogs",
                        "Purge application logs (retention)",
                        monitoringLogRetentionCron,
                        zoneId));
        out.add(
                buildForCron(
                        "removeAdsFromExpiredSubscriptionsLock",
                        "Remove ads from expired subscriptions",
                        removeExpiredAdsCron,
                        zoneId));
        out.add(
                buildForCron(
                        "sendSubscriptionExpirationEmailLock",
                        "Subscription expiry reminder emails",
                        expiryEmailsCron,
                        zoneId));
        out.add(buildForCron("cleanupUnusedAds", "Unused approved ads retention (S3 + DB)", cleanupAdsCron, zoneId));
        out.add(
                buildForFixedDelay(
                        "monitoringWorker",
                        "Monitoring worker (heartbeats + Kasa a cada monitoring.worker.interval-ms)",
                        monitoringWorkerIntervalMs));
        out.add(
                buildForFixedDelay(
                        "boxConnectivityProbe",
                        "Box connectivity probe (TCP, Box ping logs tab)",
                        boxConnectivityProbeIntervalMs));
        return out;
    }

    private SchedulerJobStatusResponseDto buildForCron(String jobId, String title, String cronExpression, ZoneId zoneId) {
        Optional<SchedulerJobRunEntity> last = schedulerJobRunRepository.findFirstByJobIdOrderByStartedAtDesc(jobId);
        Instant next = computeNextCronFire(cronExpression, zoneId);
        return toDto(jobId, title, "CRON", cronExpression, zoneId.getId(), null, last, next);
    }

    private SchedulerJobStatusResponseDto buildForFixedDelay(String jobId, String title, long intervalMs) {
        Optional<SchedulerJobRunEntity> last = schedulerJobRunRepository.findFirstByJobIdOrderByStartedAtDesc(jobId);
        Instant next =
                last.flatMap(SchedulerOverviewService::endedOrStarted)
                        .map(t -> t.plusMillis(intervalMs))
                        .orElse(null);
        return toDto(jobId, title, "FIXED_DELAY", null, null, intervalMs, last, next);
    }

    private static Optional<Instant> endedOrStarted(SchedulerJobRunEntity row) {
        if (row.getEndedAt() != null) {
            return Optional.of(row.getEndedAt());
        }
        return Optional.ofNullable(row.getStartedAt());
    }

    private SchedulerJobStatusResponseDto toDto(
            String jobId,
            String title,
            String kind,
            String cronExpression,
            String zone,
            Long fixedDelayMillis,
            Optional<SchedulerJobRunEntity> last,
            Instant nextExecutionEstimated) {
        SchedulerJobStatusResponseDto dto = new SchedulerJobStatusResponseDto();
        dto.setJobId(jobId);
        dto.setTitle(title);
        dto.setScheduleKind(kind);
        dto.setCronExpression(cronExpression);
        dto.setZone(zone);
        dto.setFixedDelayMillis(fixedDelayMillis);
        dto.setNextExecutionEstimated(nextExecutionEstimated);
        last.ifPresent(
                row -> {
                    dto.setLastStartedAt(row.getStartedAt());
                    dto.setLastEndedAt(row.getEndedAt());
                    dto.setLastStatus(row.getStatus());
                    if (row.getStartedAt() != null && row.getEndedAt() != null) {
                        dto.setLastDurationMillis(
                                Duration.between(row.getStartedAt(), row.getEndedAt()).toMillis());
                    } else if (SchedulerJobRunStatus.RUNNING.equals(row.getStatus())
                            && row.getStartedAt() != null) {
                        dto.setLastDurationMillis(
                                Duration.between(row.getStartedAt(), Instant.now()).toMillis());
                    }
                    dto.setLastRunSummary(row.getResultSummary());
                });
        return dto;
    }

    private Instant computeNextCronFire(String expression, ZoneId zoneId) {
        try {
            CronParser parser =
                    new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING));
            Cron cron = parser.parse(expression);
            ExecutionTime executionTime = ExecutionTime.forCron(cron);
            ZonedDateTime now = ZonedDateTime.now(zoneId);
            Optional<ZonedDateTime> next = executionTime.nextExecution(now);
            return next.map(ZonedDateTime::toInstant).orElse(null);
        } catch (Exception ex) {
            return null;
        }
    }
}
