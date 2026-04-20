package com.telas.services.impl;

import com.telas.dtos.request.StatusBoxMonitorsRequestDto;
import com.telas.enums.AdminEmailAlertCategory;
import com.telas.enums.DefaultStatus;
import com.telas.enums.NotificationReference;
import com.telas.monitoring.entities.IncidentEntity;
import com.telas.monitoring.entities.SmartPlugEntity;
import com.telas.monitoring.plug.PlugReading;
import com.telas.monitoring.repositories.IncidentEntityRepository;
import com.telas.monitoring.state.SmartPlugThresholdState;
import com.telas.services.AdminMonitoringNotificationService;
import com.telas.services.HealthUpdateService;
import com.telas.services.SmartPlugRulesService;
import com.telas.shared.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SmartPlugRulesServiceImpl implements SmartPlugRulesService {

    private final IncidentEntityRepository incidentEntityRepository;
    private final SmartPlugThresholdState thresholdState;
    private final AdminMonitoringNotificationService adminMonitoringNotificationService;
    private final HealthUpdateService healthUpdateService;

    @Value("${monitoring.kasa.power-below-watts:5.0}")
    private double powerBelowWatts;

    @Value("${monitoring.kasa.min-readings-below:3}")
    private int minReadingsBelow;

    @Value("${monitoring.kasa.deactivate-monitor-on-incident-types:POWER_LOSS}")
    private String deactivateMonitorOnIncidentTypesRaw;

    private Set<String> deactivateMonitorOnIncidentTypes() {
        if (!StringUtils.hasText(deactivateMonitorOnIncidentTypesRaw)) {
            return Set.of();
        }
        return Arrays.stream(deactivateMonitorOnIncidentTypesRaw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    @Transactional
    public void evaluate(SmartPlugEntity plug, PlugReading reading, boolean heartbeatStale) {
        if (!reading.reachable()) {
            thresholdState.resetLowPower(plug.getId());
            if (!heartbeatStale) {
                Map<String, Object> details = baseDetails(plug, reading, heartbeatStale);
                details.put(
                        "hypothesis",
                        "Tomada incontactável com heartbeat da box recente; verificar rede até ao plug ou corte de energia no equipamento.");
                maybeCreateIncident(plug, reading, "OTHER", "WARNING", details);
            }
            return;
        }

        Boolean relay = reading.relayOn();
        if (Boolean.FALSE.equals(relay)) {
            thresholdState.resetLowPower(plug.getId());
            Map<String, Object> details = baseDetails(plug, reading, heartbeatStale);
            details.put("hypothesis", "Relay reported off (manual switch or automation).");
            maybeCreateIncident(plug, reading, "MONITOR_OFF", "INFO", details);
            return;
        }

        Double p = reading.powerWatts();
        if (p != null && p < powerBelowWatts && !heartbeatStale) {
            int streak = thresholdState.incrementLowPower(plug.getId());
            if (streak >= minReadingsBelow) {
                Map<String, Object> details = baseDetails(plug, reading, heartbeatStale);
                details.put(
                        "hypothesis",
                        "Relay ligado mas potência abaixo do limiar (várias leituras); possível perda de carga/corrente no ramo do monitor ou falha a jusante.");
                details.put("lowPowerStreak", streak);
                maybeCreateIncident(plug, reading, "POWER_LOSS", "CRITICAL", details);
            }
        } else {
            thresholdState.resetLowPower(plug.getId());
        }
    }

    private Map<String, Object> baseDetails(
            SmartPlugEntity plug, PlugReading reading, boolean heartbeatStale) {
        Map<String, Object> details = new HashMap<>();
        details.put("source", "KASA_PLUG");
        details.put("smartPlugId", plug.getId().toString());
        details.put("macAddress", plug.getMacAddress());
        details.put("heartbeatStale", heartbeatStale);
        details.put("reachable", reading.reachable());
        details.put("relayOn", reading.relayOn());
        details.put("powerWatts", reading.powerWatts());
        details.put("confidence", heartbeatStale ? "low" : "medium");
        return details;
    }

    private void maybeCreateIncident(
            SmartPlugEntity plug,
            PlugReading reading,
            String incidentType,
            String severity,
            Map<String, Object> details) {
        UUID monitorId = plug.getMonitor() != null ? plug.getMonitor().getId() : null;
        UUID boxId = resolveIncidentBoxId(plug);
        if (monitorId == null && boxId == null) {
            return;
        }
        if (monitorId != null) {
            if (incidentEntityRepository.existsByMonitor_IdAndIncidentTypeAndClosedAtIsNull(
                    monitorId, incidentType)) {
                return;
            }
        } else if (incidentEntityRepository.existsByBox_IdAndIncidentTypeAndClosedAtIsNull(
                boxId, incidentType)) {
            return;
        }
        IncidentEntity incident = new IncidentEntity();
        incident.setIncidentType(incidentType);
        incident.setSeverity(severity);
        incident.setMonitor(plug.getMonitor());
        if (plug.getMonitor() != null) {
            incident.setBox(plug.getMonitor().getBox());
        } else {
            incident.setBox(plug.getBox());
        }
        incident.setOpenedAt(Instant.now());
        incident.setDetailsJson(details);
        incidentEntityRepository.save(incident);
        maybeDeactivateMonitorAfterPlugIncident(plug, incidentType);
        notifySmartPlugAdmins(plug, incidentType, severity, reading, details);
    }

    private void maybeDeactivateMonitorAfterPlugIncident(SmartPlugEntity plug, String incidentType) {
        if (plug.getMonitor() == null) {
            return;
        }
        Set<String> types = deactivateMonitorOnIncidentTypes();
        if (types.isEmpty() || !types.contains(incidentType)) {
            return;
        }
        StatusBoxMonitorsRequestDto dto = new StatusBoxMonitorsRequestDto();
        dto.setMonitorId(plug.getMonitor().getId());
        dto.setStatus(DefaultStatus.INACTIVE);
        healthUpdateService.applyHealthUpdate(dto);
    }

    private void notifySmartPlugAdmins(
            SmartPlugEntity plug,
            String incidentType,
            String severity,
            PlugReading reading,
            Map<String, Object> details) {
        AdminEmailAlertCategory category =
                "MONITOR_OFF".equals(incidentType)
                        ? AdminEmailAlertCategory.SMART_PLUG_RELAY_OFF
                        : AdminEmailAlertCategory.SMART_PLUG_UNREACHABLE_OR_POWER;
        Map<String, String> params = new HashMap<>();
        params.put("monitorAddress", resolveMonitorAddressLabel(plug));
        params.put("incidentType", incidentType);
        params.put("severity", severity);
        params.put("boxIp", resolveBoxIpLabel(plug));
        params.put("notifiedAt", DateUtils.formatInstantToUsDateTime(Instant.now()));
        Object hyp = details.get("hypothesis");
        params.put("hypothesis", hyp != null ? hyp.toString() : "");
        Double p = reading.powerWatts();
        params.put("powerWatts", p != null ? String.valueOf(p) : "");
        Boolean r = reading.relayOn();
        params.put("relayOn", r != null ? String.valueOf(r) : "");
        adminMonitoringNotificationService.notifyAdmins(
                NotificationReference.SMART_PLUG_INCIDENT, params, category);
    }

    private static String resolveMonitorAddressLabel(SmartPlugEntity plug) {
        if (plug.getMonitor() != null
                && plug.getMonitor().getAddress() != null) {
            return plug.getMonitor().getAddress().getCoordinatesParams();
        }
        if (plug.getBox() != null && plug.getBox().getBoxAddress() != null) {
            return "Box IP " + plug.getBox().getBoxAddress().getIp();
        }
        return "Unknown";
    }

    private static String resolveBoxIpLabel(SmartPlugEntity plug) {
        if (plug.getMonitor() != null
                && plug.getMonitor().getBox() != null
                && plug.getMonitor().getBox().getBoxAddress() != null) {
            return plug.getMonitor().getBox().getBoxAddress().getIp();
        }
        if (plug.getBox() != null && plug.getBox().getBoxAddress() != null) {
            return plug.getBox().getBoxAddress().getIp();
        }
        return "";
    }

    private static UUID resolveIncidentBoxId(SmartPlugEntity plug) {
        if (plug.getBox() != null) {
            return plug.getBox().getId();
        }
        if (plug.getMonitor() != null && plug.getMonitor().getBox() != null) {
            return plug.getMonitor().getBox().getId();
        }
        return null;
    }
}

