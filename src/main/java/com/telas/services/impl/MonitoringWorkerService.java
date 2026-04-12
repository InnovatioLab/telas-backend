package com.telas.services.impl;

import com.telas.dtos.request.StatusBoxMonitorsRequestDto;
import com.telas.entities.Box;
import com.telas.enums.DefaultStatus;
import com.telas.monitoring.KasMonitoringCheckRunner;
import com.telas.monitoring.entities.BoxHeartbeatEntity;
import com.telas.monitoring.repositories.BoxHeartbeatEntityRepository;
import com.telas.services.HealthUpdateService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonitoringWorkerService {

    private final BoxHeartbeatEntityRepository boxHeartbeatEntityRepository;
    private final HealthUpdateService healthUpdateService;
    private final KasMonitoringCheckRunner kasMonitoringCheckRunner;

    @Value("${monitoring.heartbeat.stale-seconds:180}")
    private long staleSeconds;

    @Value("${monitoring.kasa.enabled:true}")
    private boolean kasaEnabled;

    @Scheduled(fixedDelayString = "${monitoring.worker.interval-ms:60000}")
    @SchedulerLock(name = "monitoringWorker", lockAtMostFor = "PT50S", lockAtLeastFor = "PT5S")
    @Transactional
    public void runChecks() {
        Instant cutoff = Instant.now().minusSeconds(staleSeconds);
        List<BoxHeartbeatEntity> stale = boxHeartbeatEntityRepository.findStaleHeartbeats(cutoff);
        for (BoxHeartbeatEntity h : stale) {
            Box box = h.getBox();
            if (!box.isActive()) {
                continue;
            }
            StatusBoxMonitorsRequestDto dto = new StatusBoxMonitorsRequestDto();
            dto.setIp(box.getBoxAddress().getIp());
            dto.setStatus(DefaultStatus.INACTIVE);
            dto.setIncidentType("HEARTBEAT_STALE");
            dto.setIncidentSeverity("CRITICAL");
            healthUpdateService.applyHealthUpdate(dto);
        }
        if (kasaEnabled) {
            kasMonitoringCheckRunner.runKasaChecks();
        }
    }
}
