package com.telas.services.impl;

import com.telas.entities.Box;
import com.telas.entities.Monitor;
import com.telas.monitoring.entities.IncidentEntity;
import com.telas.monitoring.repositories.IncidentEntityRepository;
import com.telas.repositories.BoxRepository;
import com.telas.repositories.MonitorRepository;
import com.telas.services.HeartbeatRecoveryService;
import com.telas.shared.constants.MonitoringIncidentTypes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HeartbeatRecoveryServiceImpl implements HeartbeatRecoveryService {

    private final IncidentEntityRepository incidentEntityRepository;
    private final BoxRepository boxRepository;
    private final MonitorRepository monitorRepository;

    @Override
    @Transactional
    public void recoverAfterSuccessfulHeartbeat(Box box) {
        List<IncidentEntity> open =
                incidentEntityRepository.findAllByBox_IdAndIncidentTypeInAndClosedAtIsNull(
                        box.getId(),
                        List.of(
                                MonitoringIncidentTypes.HEARTBEAT_STALE,
                                MonitoringIncidentTypes.HEARTBEAT_NEVER_SEEN,
                                MonitoringIncidentTypes.CONNECTIVITY_PROBE_FAILED));
        if (open.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        for (IncidentEntity inc : open) {
            inc.setClosedAt(now);
            incidentEntityRepository.save(inc);
        }
        Box managed = boxRepository.findById(box.getId()).orElse(box);
        managed.setActive(true);
        List<Monitor> monitors = managed.getMonitors();
        if (monitors != null && !monitors.isEmpty()) {
            for (Monitor m : monitors) {
                m.setActive(true);
            }
            monitorRepository.saveAll(monitors);
        }
        boxRepository.save(managed);
    }
}
