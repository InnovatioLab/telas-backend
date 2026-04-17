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
import com.telas.shared.utils.BoxScriptVersionUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    @Value("${monitoring.box-script.target-version:}")
    private String configuredTargetBoxScriptVersion;

    @Override
    @Transactional(readOnly = true)
    public List<MonitoringTestingRowDto> getOverview() {
        List<Box> boxes = boxRepository.findAllForTestingOverview();
        List<SmartPlugEntity> allPlugs = smartPlugEntityRepository.findAllWithMonitor();
        Map<UUID, SmartPlugEntity> plugByMonitor =
                allPlugs.stream()
                        .filter(p -> p.getMonitor() != null)
                        .collect(
                                Collectors.toMap(
                                        p -> p.getMonitor().getId(),
                                        Function.identity(),
                                        (a, b) -> a));
        Map<UUID, SmartPlugEntity> plugByBox =
                allPlugs.stream()
                        .filter(p -> p.getBox() != null)
                        .collect(
                                Collectors.toMap(
                                        p -> p.getBox().getId(),
                                        Function.identity(),
                                        (a, b) -> a));
        Instant now = Instant.now();
        Instant cutoff = now.minusSeconds(staleSeconds);
        List<MonitoringTestingRowDto> rows = new ArrayList<>();
        String targetVer = normalizeTargetVersion(configuredTargetBoxScriptVersion);
        for (Box box : boxes) {
            Optional<BoxHeartbeatEntity> hb = boxHeartbeatEntityRepository.findByBox_Id(box.getId());
            Instant lastSeen = hb.map(BoxHeartbeatEntity::getLastSeenAt).orElse(null);
            String reportedVer = hb.map(BoxHeartbeatEntity::getReportedVersion).orElse(null);
            String gitSha = extractMetadataString(hb.map(BoxHeartbeatEntity::getMetadataJson).orElse(null), "gitSha");
            String buildId = extractMetadataString(hb.map(BoxHeartbeatEntity::getMetadataJson).orElse(null), "buildId");
            String status = resolveHeartbeatStatus(lastSeen, cutoff);
            boolean online = HEARTBEAT_STATUS_ONLINE.equals(status);
            List<Monitor> monitors = box.getMonitors();
            SmartPlugEntity boxPlug = plugByBox.get(box.getId());
            if (monitors == null || monitors.isEmpty()) {
                rows.add(
                        buildRow(
                                box,
                                null,
                                null,
                                boxPlug,
                                lastSeen,
                                online,
                                status,
                                reportedVer,
                                targetVer,
                                gitSha,
                                buildId));
                continue;
            }
            for (Monitor monitor : monitors) {
                SmartPlugEntity monitorPlug = plugByMonitor.get(monitor.getId());
                rows.add(
                        buildRow(
                                box,
                                monitor,
                                monitorPlug,
                                boxPlug,
                                lastSeen,
                                online,
                                status,
                                reportedVer,
                                targetVer,
                                gitSha,
                                buildId));
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
        String reportedVer = hb.map(BoxHeartbeatEntity::getReportedVersion).orElse(null);
        String gitSha = extractMetadataString(hb.map(BoxHeartbeatEntity::getMetadataJson).orElse(null), "gitSha");
        String buildId = extractMetadataString(hb.map(BoxHeartbeatEntity::getMetadataJson).orElse(null), "buildId");
        String targetVer = normalizeTargetVersion(configuredTargetBoxScriptVersion);
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
                .reportedBoxScriptVersion(reportedVer)
                .targetBoxScriptVersion(StringUtils.hasText(targetVer) ? targetVer : null)
                .boxScriptVersionStatus(
                        BoxScriptVersionUtils.resolveStatus(reportedVer, targetVer))
                .reportedGitSha(gitSha)
                .reportedBuildId(buildId)
                .build();
    }

    private static String normalizeTargetVersion(String raw) {
        return StringUtils.hasText(raw) ? raw.trim() : "";
    }

    private static String extractMetadataString(Map<String, Object> metadata, String key) {
        if (metadata == null || key == null) {
            return null;
        }
        Object v = metadata.get(key);
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v);
        return StringUtils.hasText(s) ? s : null;
    }

    private static MonitoringTestingRowDto buildRow(
            Box box,
            Monitor monitor,
            SmartPlugEntity monitorPlug,
            SmartPlugEntity boxPlug,
            Instant lastSeen,
            boolean heartbeatOnline,
            String heartbeatStatus,
            String reportedBoxScriptVersion,
            String targetBoxScriptVersion,
            String reportedGitSha,
            String reportedBuildId) {
        String monitorSummary = null;
        Boolean monitorActive = null;
        UUID monitorId = null;
        if (monitor != null) {
            monitorId = monitor.getId();
            monitorActive = monitor.isActive();
            Address addr = monitor.getAddress();
            monitorSummary = addr != null ? addr.getCoordinatesParams() : null;
        }
        UUID smartPlugId = monitorPlug != null ? monitorPlug.getId() : null;
        String mac = monitorPlug != null ? monitorPlug.getMacAddress() : null;
        String vendor = monitorPlug != null ? monitorPlug.getVendor() : null;
        Boolean plugEnabled = monitorPlug != null ? monitorPlug.isEnabled() : null;
        UUID boxSmartPlugId = boxPlug != null ? boxPlug.getId() : null;
        String boxMac = boxPlug != null ? boxPlug.getMacAddress() : null;
        String boxVendor = boxPlug != null ? boxPlug.getVendor() : null;
        Boolean boxPlugEnabled = boxPlug != null ? boxPlug.isEnabled() : null;
        String target = normalizeTargetVersion(targetBoxScriptVersion);
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
                .boxSmartPlugId(boxSmartPlugId)
                .boxSmartPlugMac(boxMac)
                .boxSmartPlugVendor(boxVendor)
                .boxSmartPlugEnabled(boxPlugEnabled)
                .reportedBoxScriptVersion(reportedBoxScriptVersion)
                .targetBoxScriptVersion(StringUtils.hasText(target) ? target : null)
                .boxScriptVersionStatus(
                        BoxScriptVersionUtils.resolveStatus(reportedBoxScriptVersion, target))
                .reportedGitSha(reportedGitSha)
                .reportedBuildId(reportedBuildId)
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
