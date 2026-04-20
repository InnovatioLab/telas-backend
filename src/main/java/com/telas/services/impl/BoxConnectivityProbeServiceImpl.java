package com.telas.services.impl;

import com.telas.dtos.response.BoxConnectivityProbeRowResponseDto;
import com.telas.entities.Address;
import com.telas.entities.Box;
import com.telas.entities.Monitor;
import com.telas.monitoring.entities.BoxConnectivityProbeEntity;
import com.telas.monitoring.repositories.BoxConnectivityProbeEntityRepository;
import com.telas.dtos.request.StatusBoxMonitorsRequestDto;
import com.telas.enums.DefaultStatus;
import com.telas.repositories.BoxRepository;
import com.telas.services.ApplicationLogService;
import com.telas.services.BoxConnectivityProbeService;
import com.telas.services.BoxTailscalePingOutcome;
import com.telas.services.BoxTailscalePingService;
import com.telas.services.HealthUpdateService;
import com.telas.services.HeartbeatRecoveryService;
import com.telas.shared.constants.MonitoringIncidentTypes;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoxConnectivityProbeServiceImpl implements BoxConnectivityProbeService {

    private static final Logger log = LoggerFactory.getLogger(BoxConnectivityProbeServiceImpl.class);

    private final BoxRepository boxRepository;
    private final BoxTailscalePingService boxTailscalePingService;
    private final BoxConnectivityProbeEntityRepository boxConnectivityProbeEntityRepository;
    private final HealthUpdateService healthUpdateService;
    private final HeartbeatRecoveryService heartbeatRecoveryService;
    private final ApplicationLogService applicationLogService;

    @Value("${monitoring.box-connectivity-probe.enabled:true}")
    private boolean probeEnabled;

    @Value("${monitoring.box-connectivity-probe.drives-box-active-state:true}")
    private boolean drivesBoxActiveState;

    @Override
    @Transactional(readOnly = true)
    public List<BoxConnectivityProbeRowResponseDto> listProbeRows() {
        List<Box> boxes = boxRepository.findAllForTestingOverview();
        List<UUID> ids = boxes.stream().map(Box::getId).toList();
        Map<UUID, BoxConnectivityProbeEntity> byBox =
                boxConnectivityProbeEntityRepository.findAllById(ids).stream()
                        .collect(Collectors.toMap(BoxConnectivityProbeEntity::getBoxId, e -> e, (a, b) -> a));
        List<BoxConnectivityProbeRowResponseDto> rows = new ArrayList<>();
        for (Box box : boxes) {
            Optional<BoxConnectivityProbeEntity> probe = Optional.ofNullable(byBox.get(box.getId()));
            List<Monitor> monitors = box.getMonitors();
            if (monitors == null || monitors.isEmpty()) {
                rows.add(buildRow(box, null, probe));
                continue;
            }
            for (Monitor monitor : monitors) {
                rows.add(buildRow(box, monitor, probe));
            }
        }
        return rows;
    }

    private static BoxConnectivityProbeRowResponseDto buildRow(
            Box box, Monitor monitor, Optional<BoxConnectivityProbeEntity> probe) {
        UUID monitorId = monitor != null ? monitor.getId() : null;
        String monitorSummary = null;
        if (monitor != null) {
            Address addr = monitor.getAddress();
            monitorSummary = addr != null ? addr.getCoordinatesParams() : null;
        }
        String boxIp = box.getBoxAddress() != null ? box.getBoxAddress().getIp() : null;
        return BoxConnectivityProbeRowResponseDto.builder()
                .boxId(box.getId())
                .boxIp(boxIp)
                .monitorId(monitorId)
                .monitorAddressSummary(monitorSummary)
                .lastProbeAt(probe.map(BoxConnectivityProbeEntity::getLastProbeAt).orElse(null))
                .reachable(probe.map(BoxConnectivityProbeEntity::isReachable).orElse(null))
                .probeDetail(probe.map(BoxConnectivityProbeEntity::getProbeDetail).orElse(null))
                .build();
    }

    @Override
    @Transactional
    public void runScheduledProbes() {
        if (!probeEnabled) {
            return;
        }
        executeProbeCycle();
    }

    @Override
    @Transactional
    public void runProbesNow() {
        executeProbeCycle();
    }

    private void executeProbeCycle() {
        List<Box> boxes = boxRepository.findAllForTestingOverview();
        int ok = 0;
        int fail = 0;
        Instant now = Instant.now();
        for (Box box : boxes) {
            String ip = box.getBoxAddress() != null ? box.getBoxAddress().getIp() : null;
            BoxTailscalePingOutcome outcome = boxTailscalePingService.pingBoxAddressIp(ip);
            boolean reachable = outcome.attempted() && outcome.reachable();
            if (reachable) {
                ok++;
            } else {
                fail++;
            }
            String detail = outcome.detail();
            if (detail != null && detail.length() > 2000) {
                detail = detail.substring(0, 2000) + "…";
            }
            BoxConnectivityProbeEntity row =
                    boxConnectivityProbeEntityRepository
                            .findById(box.getId())
                            .orElseGet(
                                    () -> {
                                        BoxConnectivityProbeEntity e = new BoxConnectivityProbeEntity();
                                        e.setBoxId(box.getId());
                                        return e;
                                    });
            row.setLastProbeAt(now);
            row.setReachable(reachable);
            row.setProbeDetail(detail);
            row.setBoxIp(ip);
            row.setUpdatedAt(now);
            boxConnectivityProbeEntityRepository.save(row);
            if (reachable) {
                log.debug(
                        "box.connectivity.probe boxId={} ip={} reachable={} detail={}",
                        box.getId(),
                        ip,
                        true,
                        detail);
            } else {
                log.warn(
                        "box.connectivity.probe.failed boxId={} ip={} detail={}",
                        box.getId(),
                        ip,
                        detail);
            }
            applyActiveStateFromProbeIfEnabled(box, ip, outcome, reachable);
        }
        if (fail > 0) {
            log.info(
                    "box.connectivity.probe.summary totalBoxes={} reachableCount={} unreachableCount={}",
                    boxes.size(),
                    ok,
                    fail);
        } else {
            log.debug(
                    "box.connectivity.probe.summary totalBoxes={} reachableCount={} unreachableCount={}",
                    boxes.size(),
                    ok,
                    0);
        }
    }

    private void applyActiveStateFromProbeIfEnabled(
            Box box, String ip, BoxTailscalePingOutcome outcome, boolean reachable) {
        if (!drivesBoxActiveState || !outcome.attempted()) {
            return;
        }
        boolean desiredActive = reachable;
        if (desiredActive != box.isActive()) {
            StatusBoxMonitorsRequestDto dto = new StatusBoxMonitorsRequestDto();
            dto.setIp(ip);
            dto.setStatus(desiredActive ? DefaultStatus.ACTIVE : DefaultStatus.INACTIVE);
            if (!desiredActive) {
                dto.setIncidentType(MonitoringIncidentTypes.CONNECTIVITY_PROBE_FAILED);
                dto.setIncidentSeverity("CRITICAL");
                Map<String, Object> logMeta = new HashMap<>();
                logMeta.put("boxId", box.getId().toString());
                logMeta.put("boxIp", ip != null ? ip : "");
                logMeta.put("incidentType", MonitoringIncidentTypes.CONNECTIVITY_PROBE_FAILED);
                String label = ip != null && !ip.isBlank() ? ip : box.getId().toString();
                applicationLogService.persistSystemLog(
                        "WARN",
                        String.format("CONNECTIVITY_PROBE: box %s inacessível pela sonda; estado inativo.", label),
                        "MONITORING",
                        logMeta);
            }
            healthUpdateService.applyHealthUpdate(dto);
        }
        if (desiredActive) {
            heartbeatRecoveryService.recoverAfterSuccessfulHeartbeat(box);
        }
    }
}
