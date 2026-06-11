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
import com.telas.services.BoxConnectivityProbeService;
import com.telas.services.BoxTailscalePingOutcome;
import com.telas.services.BoxTailscalePingService;
import com.telas.services.HealthUpdateService;
import com.telas.services.HeartbeatRecoveryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    private final SideApiAlertManager sideApiAlertManager;

    private final Map<UUID, Integer> consecutiveProbeFailures = new ConcurrentHashMap<>();

    @Value("${monitoring.box-connectivity-probe.enabled:true}")
    private boolean probeEnabled;

    @Value("${monitoring.box-connectivity-probe.drives-box-active-state:true}")
    private boolean drivesBoxActiveState;

    @Value("${monitoring.box-connectivity-probe.deactivate-after-consecutive-failures:2}")
    private int deactivateAfterConsecutiveFailures;

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
        Instant now = Instant.now();
        int ok = 0;
        int fail = 0;
        for (Box box : boxes) {
            if (probeBox(box, now)) {
                ok++;
            } else {
                fail++;
            }
        }
        logProbeSummary(boxes.size(), ok, fail);
    }

    private boolean probeBox(Box box, Instant now) {
        String ip = box.getBoxAddress() != null ? box.getBoxAddress().getIp() : null;
        BoxTailscalePingOutcome outcome = boxTailscalePingService.pingBoxAddressIp(ip);
        boolean reachable = outcome.attempted() && outcome.reachable();

        persistProbeResult(box, ip, outcome, reachable, now);

        if (reachable) {
            sideApiAlertManager.checkAndAlert(box.getId(), ip, now);
        }
        applyActiveStateFromProbeIfEnabled(box, ip, outcome, reachable);
        return reachable;
    }

    private void persistProbeResult(Box box, String ip, BoxTailscalePingOutcome outcome, boolean reachable, Instant now) {
        String detail = outcome.detail();
        if (detail != null && detail.length() > 2000) {
            detail = detail.substring(0, 2000) + "…";
        }
        BoxConnectivityProbeEntity row =
                boxConnectivityProbeEntityRepository
                        .findById(box.getId())
                        .orElseGet(() -> {
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
            log.debug("box.connectivity.probe boxId={} ip={} reachable=true detail={}", box.getId(), ip, detail);
        } else {
            log.warn("box.connectivity.probe.failed boxId={} ip={} detail={}", box.getId(), ip, detail);
        }
    }

    private void applyActiveStateFromProbeIfEnabled(
            Box box, String ip, BoxTailscalePingOutcome outcome, boolean reachable) {
        if (!drivesBoxActiveState || !outcome.attempted()) {
            return;
        }
        UUID boxId = box.getId();
        if (reachable) {
            consecutiveProbeFailures.remove(boxId);
            if (!box.isActive()) {
                StatusBoxMonitorsRequestDto dto = new StatusBoxMonitorsRequestDto();
                dto.setIp(ip);
                dto.setStatus(DefaultStatus.ACTIVE);
                healthUpdateService.applyHealthUpdate(dto);
            }
            heartbeatRecoveryService.recoverAfterSuccessfulHeartbeat(box);
        } else {
            int failures = consecutiveProbeFailures.merge(boxId, 1, Integer::sum);
            if (box.isActive() && failures >= deactivateAfterConsecutiveFailures) {
                log.warn("box.connectivity.probe: {} falhas consecutivas — desativando box. boxId={} ip={}",
                        failures, boxId, ip);
                consecutiveProbeFailures.remove(boxId);
                StatusBoxMonitorsRequestDto dto = new StatusBoxMonitorsRequestDto();
                dto.setIp(ip);
                dto.setStatus(DefaultStatus.INACTIVE);
                healthUpdateService.applyHealthUpdate(dto);
            } else {
                log.warn("box.connectivity.probe: box inacessível ({}/{} falhas). boxId={} ip={}",
                        failures, deactivateAfterConsecutiveFailures, boxId, ip);
            }
        }
    }

    private void logProbeSummary(int total, int ok, int fail) {
        if (fail > 0) {
            log.info("box.connectivity.probe.summary totalBoxes={} reachableCount={} unreachableCount={}",
                    total, ok, fail);
        } else {
            log.debug("box.connectivity.probe.summary totalBoxes={} reachableCount={} unreachableCount={}",
                    total, ok, 0);
        }
    }
}
