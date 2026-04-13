package com.telas.services;

import com.telas.monitoring.crypto.AesTextEncryptionService;
import com.telas.monitoring.entities.CheckRunEntity;
import com.telas.monitoring.entities.IncidentEntity;
import com.telas.monitoring.entities.SmartPlugEntity;
import com.telas.monitoring.plug.PlugReading;
import com.telas.monitoring.plug.SmartPlugClient;
import com.telas.monitoring.repositories.BoxHeartbeatEntityRepository;
import com.telas.monitoring.repositories.CheckRunEntityRepository;
import com.telas.monitoring.repositories.IncidentEntityRepository;
import com.telas.monitoring.repositories.SmartPlugEntityRepository;
import com.telas.monitoring.state.SmartPlugThresholdState;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SmartPlugCheckService {

    private static final Logger log = LoggerFactory.getLogger(SmartPlugCheckService.class);

    private final SmartPlugEntityRepository smartPlugEntityRepository;
    private final SmartPlugClient smartPlugClient;
    private final AesTextEncryptionService encryptionService;
    private final BoxHeartbeatEntityRepository boxHeartbeatEntityRepository;
    private final IncidentEntityRepository incidentEntityRepository;
    private final CheckRunEntityRepository checkRunEntityRepository;
    private final SmartPlugThresholdState thresholdState;

    @Value("${monitoring.kasa.raise-incidents:true}")
    private boolean raiseIncidents;

    @Value("${monitoring.kasa.power-below-watts:5.0}")
    private double powerBelowWatts;

    @Value("${monitoring.kasa.min-readings-below:3}")
    private int minReadingsBelow;

    @Value("${monitoring.heartbeat.stale-seconds:180}")
    private long staleSeconds;

    @Transactional
    public void runAllChecks() {
        List<SmartPlugEntity> plugs = smartPlugEntityRepository.findAllEnabledWithMonitorAndBox();
        for (SmartPlugEntity plug : plugs) {
            try {
                runSingle(plug);
            } catch (Exception e) {
                log.warn("Smart plug check failed plugId={}", plug.getId(), e);
            }
        }
    }

    private void runSingle(SmartPlugEntity plug) {
        String password = resolvePassword(plug);
        PlugReading reading = smartPlugClient.read(plug, password);
        persistCheckRun(plug, reading);
        if (!raiseIncidents) {
            return;
        }
        boolean heartbeatStale = isHeartbeatStale(plug);
        evaluateRules(plug, reading, heartbeatStale);
    }

    private String resolvePassword(SmartPlugEntity plug) {
        if (!encryptionService.isConfigured() || plug.getPasswordCipher() == null) {
            return null;
        }
        return encryptionService.decrypt(plug.getPasswordCipher());
    }

    private boolean isHeartbeatStale(SmartPlugEntity plug) {
        if (plug.getMonitor() == null || plug.getMonitor().getBox() == null) {
            return true;
        }
        Instant cutoff = Instant.now().minusSeconds(staleSeconds);
        return boxHeartbeatEntityRepository
                .findByBox_Id(plug.getMonitor().getBox().getId())
                .map(h -> h.getLastSeenAt().isBefore(cutoff))
                .orElse(true);
    }

    private void persistCheckRun(SmartPlugEntity plug, PlugReading reading) {
        CheckRunEntity run = new CheckRunEntity();
        run.setSmartPlug(plug);
        run.setStartedAt(Instant.now());
        run.setFinishedAt(Instant.now());
        run.setSuccess(reading.reachable());
        if (!reading.reachable()) {
            run.setErrorMessage(reading.errorCode());
        }
        Map<String, Object> meta = new HashMap<>();
        meta.put("relayOn", reading.relayOn());
        meta.put("powerWatts", reading.powerWatts());
        meta.put("voltageVolts", reading.voltageVolts());
        meta.put("currentAmperes", reading.currentAmperes());
        meta.put("confidence", "medium");
        run.setMetadataJson(meta);
        checkRunEntityRepository.save(run);
    }

    private void evaluateRules(SmartPlugEntity plug, PlugReading reading, boolean heartbeatStale) {
        if (!reading.reachable()) {
            thresholdState.resetLowPower(plug.getId());
            if (!heartbeatStale) {
                Map<String, Object> details = baseDetails(plug, reading, heartbeatStale);
                details.put(
                        "hypothesis",
                        "Tomada incontactável com heartbeat da box recente; verificar rede até ao plug ou corte de energia no equipamento.");
                maybeCreateIncident(plug, "OTHER", "WARNING", details);
            }
            return;
        }

        Boolean relay = reading.relayOn();
        if (Boolean.FALSE.equals(relay)) {
            thresholdState.resetLowPower(plug.getId());
            Map<String, Object> details = baseDetails(plug, reading, heartbeatStale);
            details.put("hypothesis", "Relay reported off (manual switch or automation).");
            maybeCreateIncident(plug, "MONITOR_OFF", "INFO", details);
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
                maybeCreateIncident(plug, "POWER_LOSS", "CRITICAL", details);
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
            String incidentType,
            String severity,
            Map<String, Object> details) {
        if (plug.getMonitor() == null) {
            return;
        }
        if (incidentEntityRepository.existsByMonitor_IdAndIncidentTypeAndClosedAtIsNull(
                plug.getMonitor().getId(), incidentType)) {
            return;
        }
        IncidentEntity incident = new IncidentEntity();
        incident.setIncidentType(incidentType);
        incident.setSeverity(severity);
        incident.setMonitor(plug.getMonitor());
        incident.setBox(plug.getMonitor().getBox());
        incident.setOpenedAt(Instant.now());
        incident.setDetailsJson(details);
        incidentEntityRepository.save(incident);
    }
}
