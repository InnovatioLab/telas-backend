package com.telas.services.impl;

import com.telas.dtos.request.StatusBoxMonitorsRequestDto;
import com.telas.entities.Box;
import com.telas.enums.DefaultStatus;
import com.telas.monitoring.KasMonitoringCheckRunner;
import com.telas.monitoring.entities.BoxHeartbeatEntity;
import com.telas.monitoring.repositories.BoxHeartbeatEntityRepository;
import com.telas.repositories.BoxRepository;
import com.telas.scheduler.SchedulerJobRunContext;
import com.telas.services.HealthUpdateService;
import com.telas.services.SmartPlugIpDiscoveryService;
import com.telas.shared.constants.MonitoringIncidentTypes;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MonitoringWorkerService {

    private static final int HEARTBEAT_SUMMARY_MAX_ROWS = 50;

    private final BoxHeartbeatEntityRepository boxHeartbeatEntityRepository;
    private final BoxRepository boxRepository;
    private final HealthUpdateService healthUpdateService;
    private final KasMonitoringCheckRunner kasMonitoringCheckRunner;
    private final SmartPlugIpDiscoveryService smartPlugIpDiscoveryService;
    private final SchedulerJobRunContext schedulerJobRunContext;

    @Value("${monitoring.heartbeat.stale-seconds:180}")
    private long staleSeconds;

    @Value("${monitoring.heartbeat.never-seen-grace-seconds:600}")
    private long neverSeenGraceSeconds;

    @Value("${monitoring.kasa.enabled:true}")
    private boolean kasaEnabled;

    @Value("${monitoring.kasa.discovery.enabled:false}")
    private boolean kasDiscoveryEnabled;

    @Value("${monitoring.worker.interval-ms:60000}")
    private long kasaIntervalMs;

    @Value("${monitoring.box-connectivity-probe.drives-box-active-state:true}")
    private boolean probeDrivesBoxActiveState;

    private Instant lastKasaRunAt = Instant.now();

    @Scheduled(fixedDelayString = "${monitoring.worker.heartbeat-check-interval-ms:10000}")
    @SchedulerLock(name = "monitoringWorker", lockAtMostFor = "PT120S", lockAtLeastFor = "PT2S")
    @Transactional
    public void runChecks() {
        Instant cutoff = Instant.now().minusSeconds(staleSeconds);
        List<BoxHeartbeatEntity> stale = List.of();
        List<Map<String, String>> staleSummary = new ArrayList<>();
        int staleProcessed = 0;
        if (!probeDrivesBoxActiveState) {
            stale = boxHeartbeatEntityRepository.findStaleHeartbeats(cutoff);
            for (BoxHeartbeatEntity h : stale) {
                Box box = h.getBox();
                if (!box.isActive()) {
                    continue;
                }
                staleProcessed++;
                StatusBoxMonitorsRequestDto dto = new StatusBoxMonitorsRequestDto();
                dto.setIp(box.getBoxAddress().getIp());
                dto.setStatus(DefaultStatus.INACTIVE);
                dto.setIncidentType(MonitoringIncidentTypes.HEARTBEAT_STALE);
                dto.setIncidentSeverity("CRITICAL");
                healthUpdateService.applyHealthUpdate(dto);
                if (staleSummary.size() < HEARTBEAT_SUMMARY_MAX_ROWS) {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("boxId", box.getId().toString());
                    row.put("ip", box.getBoxAddress().getIp());
                    staleSummary.add(row);
                }
            }
        }
        Instant graceCutoff = Instant.now().minusSeconds(neverSeenGraceSeconds);
        List<Box> neverSeen = List.of();
        List<Map<String, String>> neverSeenSummary = new ArrayList<>();
        int neverSeenProcessed = 0;
        if (!probeDrivesBoxActiveState) {
            neverSeen = boxRepository.findActiveBoxesWithoutHeartbeatAfterGrace(graceCutoff);
            for (Box box : neverSeen) {
                if (!box.isActive()) {
                    continue;
                }
                neverSeenProcessed++;
                StatusBoxMonitorsRequestDto dto = new StatusBoxMonitorsRequestDto();
                dto.setIp(box.getBoxAddress().getIp());
                dto.setStatus(DefaultStatus.INACTIVE);
                dto.setIncidentType(MonitoringIncidentTypes.HEARTBEAT_NEVER_SEEN);
                dto.setIncidentSeverity("CRITICAL");
                healthUpdateService.applyHealthUpdate(dto);
                if (neverSeenSummary.size() < HEARTBEAT_SUMMARY_MAX_ROWS) {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("boxId", box.getId().toString());
                    row.put("ip", box.getBoxAddress().getIp());
                    neverSeenSummary.add(row);
                }
            }
        }
        schedulerJobRunContext.put("staleHeartbeatsProcessed", staleProcessed);
        schedulerJobRunContext.put("staleHeartbeats", staleSummary);
        if (staleProcessed > staleSummary.size()) {
            schedulerJobRunContext.put("staleHeartbeatsTruncated", true);
        }
        schedulerJobRunContext.put("neverSeenHeartbeatsProcessed", neverSeenProcessed);
        schedulerJobRunContext.put("neverSeenHeartbeats", neverSeenSummary);
        if (neverSeenProcessed > neverSeenSummary.size()) {
            schedulerJobRunContext.put("neverSeenHeartbeatsTruncated", true);
        }
        Instant now = Instant.now();
        if (kasaEnabled
                && Duration.between(lastKasaRunAt, now).toMillis() >= kasaIntervalMs) {
            lastKasaRunAt = now;
            if (kasDiscoveryEnabled) {
                schedulerJobRunContext.putAll(smartPlugIpDiscoveryService.runDiscoveryCycle());
            }
            schedulerJobRunContext.putAll(kasMonitoringCheckRunner.runKasaChecks());
        }
    }
}
