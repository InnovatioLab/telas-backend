package com.telas.services.impl;

import com.telas.dtos.response.BoxHeartbeatCheckResponseDto;
import com.telas.dtos.response.MonitoringTestingRowDto;
import com.telas.entities.Address;
import com.telas.entities.Box;
import com.telas.entities.Monitor;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.monitoring.entities.BoxHeartbeatEntity;
import com.telas.monitoring.entities.SmartPlugEntity;
import com.telas.monitoring.repositories.BoxHeartbeatEntityRepository;
import com.telas.monitoring.repositories.SmartPlugEntityRepository;
import com.telas.repositories.BoxRepository;
import com.telas.services.MonitoringTestingService;
import com.telas.shared.constants.valitation.BoxValidationMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonitoringTestingServiceImpl implements MonitoringTestingService {

    public static final String HEARTBEAT_STATUS_ONLINE = "ONLINE";
    public static final String HEARTBEAT_STATUS_STALE = "STALE";
    public static final String HEARTBEAT_STATUS_MISSING = "MISSING";

    private final BoxRepository boxRepository;
    private final BoxHeartbeatEntityRepository boxHeartbeatEntityRepository;
    private final SmartPlugEntityRepository smartPlugEntityRepository;

    @Value("${monitoring.heartbeat.stale-seconds:180}")
    private long staleSeconds;

    @Override
    @Transactional(readOnly = true)
    public List<MonitoringTestingRowDto> getOverview() {
        List<Box> boxes = boxRepository.findAllForTestingOverview();
        Map<UUID, SmartPlugEntity> plugByMonitor =
                smartPlugEntityRepository.findAllWithMonitor().stream()
                        .collect(
                                Collectors.toMap(
                                        p -> p.getMonitor().getId(),
                                        Function.identity(),
                                        (a, b) -> a));
        Instant now = Instant.now();
        Instant cutoff = now.minusSeconds(staleSeconds);
        List<MonitoringTestingRowDto> rows = new ArrayList<>();
        for (Box box : boxes) {
            Optional<BoxHeartbeatEntity> hb = boxHeartbeatEntityRepository.findByBox_Id(box.getId());
            Instant lastSeen = hb.map(BoxHeartbeatEntity::getLastSeenAt).orElse(null);
            String status = resolveHeartbeatStatus(lastSeen, cutoff);
            boolean online = HEARTBEAT_STATUS_ONLINE.equals(status);
            List<Monitor> monitors = box.getMonitors();
            if (monitors == null || monitors.isEmpty()) {
                rows.add(buildRow(box, null, null, lastSeen, online, status));
                continue;
            }
            for (Monitor monitor : monitors) {
                SmartPlugEntity plug = plugByMonitor.get(monitor.getId());
                rows.add(buildRow(box, monitor, plug, lastSeen, online, status));
            }
        }
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public BoxHeartbeatCheckResponseDto checkBoxHeartbeat(UUID boxId) {
        Box box =
                boxRepository
                        .findById(boxId)
                        .orElseThrow(() -> new ResourceNotFoundException(BoxValidationMessages.BOX_NOT_FOUND));
        Optional<BoxHeartbeatEntity> hb = boxHeartbeatEntityRepository.findByBox_Id(box.getId());
        Instant lastSeen = hb.map(BoxHeartbeatEntity::getLastSeenAt).orElse(null);
        Instant now = Instant.now();
        Instant cutoff = now.minusSeconds(staleSeconds);
        String status = resolveHeartbeatStatus(lastSeen, cutoff);
        boolean online = HEARTBEAT_STATUS_ONLINE.equals(status);
        Long secondsSince =
                lastSeen == null ? null : ChronoUnit.SECONDS.between(lastSeen, now);
        return BoxHeartbeatCheckResponseDto.builder()
                .boxId(box.getId())
                .lastHeartbeatAt(lastSeen)
                .secondsSinceHeartbeat(secondsSince)
                .heartbeatOnline(online)
                .heartbeatStatus(status)
                .staleAfterSeconds(staleSeconds)
                .build();
    }

    private static MonitoringTestingRowDto buildRow(
            Box box,
            Monitor monitor,
            SmartPlugEntity plug,
            Instant lastSeen,
            boolean heartbeatOnline,
            String heartbeatStatus) {
        String monitorSummary = null;
        Boolean monitorActive = null;
        UUID monitorId = null;
        if (monitor != null) {
            monitorId = monitor.getId();
            monitorActive = monitor.isActive();
            Address addr = monitor.getAddress();
            monitorSummary = addr != null ? addr.getCoordinatesParams() : null;
        }
        UUID smartPlugId = plug != null ? plug.getId() : null;
        String mac = plug != null ? plug.getMacAddress() : null;
        String vendor = plug != null ? plug.getVendor() : null;
        Boolean plugEnabled = plug != null ? plug.isEnabled() : null;
        return MonitoringTestingRowDto.builder()
                .boxId(box.getId())
                .boxIp(box.getBoxAddress() != null ? box.getBoxAddress().getIp() : null)
                .boxActive(box.isActive())
                .lastHeartbeatAt(lastSeen)
                .heartbeatOnline(heartbeatOnline)
                .heartbeatStatus(heartbeatStatus)
                .monitorId(monitorId)
                .monitorAddressSummary(monitorSummary)
                .monitorActive(monitorActive)
                .smartPlugId(smartPlugId)
                .smartPlugMac(mac)
                .smartPlugVendor(vendor)
                .smartPlugEnabled(plugEnabled)
                .build();
    }

    private static String resolveHeartbeatStatus(Instant lastSeen, Instant cutoff) {
        if (lastSeen == null) {
            return HEARTBEAT_STATUS_MISSING;
        }
        if (lastSeen.isBefore(cutoff)) {
            return HEARTBEAT_STATUS_STALE;
        }
        return HEARTBEAT_STATUS_ONLINE;
    }
}
