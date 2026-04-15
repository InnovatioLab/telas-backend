package com.telas.services;

import com.telas.entities.Box;
import com.telas.enums.NotificationReference;
import com.telas.monitoring.entities.IncidentEntity;
import com.telas.monitoring.repositories.IncidentEntityRepository;
import com.telas.repositories.ClientRepository;
import com.telas.shared.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HeartbeatRebootIncidentService {

    public static final String INCIDENT_TYPE_HOST_REBOOT = "HOST_REBOOT";

    private final IncidentEntityRepository incidentEntityRepository;
    private final ApplicationLogService applicationLogService;
    private final ClientRepository clientRepository;
    private final NotificationService notificationService;

    @Value("${monitoring.heartbeat.reboot-detection-enabled:true}")
    private boolean rebootDetectionEnabled;

    @Value("${monitoring.heartbeat.reboot-min-uptime-drop-seconds:60}")
    private long rebootMinUptimeDropSeconds;

    public void recordIfHostRebootDetected(
            Box box, Map<String, Object> previousMetadata, Map<String, Object> newMetadata) {
        if (!rebootDetectionEnabled || previousMetadata == null || newMetadata == null) {
            return;
        }
        Long prevUptime = extractHostUptimeSeconds(previousMetadata);
        Long nextUptime = extractHostUptimeSeconds(newMetadata);
        if (prevUptime == null || nextUptime == null) {
            return;
        }
        if (nextUptime >= prevUptime) {
            return;
        }
        long drop = prevUptime - nextUptime;
        if (drop < rebootMinUptimeDropSeconds) {
            return;
        }
        if (incidentEntityRepository.existsByBox_IdAndIncidentTypeAndClosedAtIsNull(
                box.getId(), INCIDENT_TYPE_HOST_REBOOT)) {
            return;
        }
        IncidentEntity incident = new IncidentEntity();
        incident.setIncidentType(INCIDENT_TYPE_HOST_REBOOT);
        incident.setSeverity("INFO");
        incident.setBox(box);
        incident.setOpenedAt(Instant.now());
        Map<String, Object> details = new HashMap<>();
        details.put("previousHostUptimeSeconds", prevUptime);
        details.put("currentHostUptimeSeconds", nextUptime);
        details.put("uptimeDropSeconds", drop);
        details.put(
                "hypothesis",
                "Queda de hostUptimeSeconds superior ao limiar configurado; provável reinício do SO.");
        incident.setDetailsJson(details);
        incidentEntityRepository.save(incident);

        String boxIp = box.getBoxAddress() != null ? box.getBoxAddress().getIp() : "";
        Map<String, Object> logMeta = new HashMap<>(details);
        logMeta.put("incidentId", incident.getId().toString());
        logMeta.put("incidentType", INCIDENT_TYPE_HOST_REBOOT);
        logMeta.put("boxId", box.getId().toString());
        applicationLogService.persistSystemLog(
                "INFO",
                String.format(
                        "HOST_REBOOT: box %s — queda de uptime %ds (reinício do SO provável).",
                        boxIp.isEmpty() ? box.getId() : boxIp,
                        drop),
                "MONITORING",
                logMeta);

        Map<String, String> notifParams = new HashMap<>();
        notifParams.put("boxIp", boxIp.isEmpty() ? box.getId().toString() : boxIp);
        notifParams.put("incidentType", INCIDENT_TYPE_HOST_REBOOT);
        notifParams.put("severity", "INFO");
        notifParams.put("uptimeDropSeconds", String.valueOf(drop));
        notifParams.put("notifiedAt", DateUtils.formatInstantToUsDateTime(Instant.now()));
        clientRepository
                .findAllAdmins()
                .forEach(
                        admin ->
                                notificationService.save(
                                        NotificationReference.MONITORING_HOST_REBOOT,
                                        admin,
                                        notifParams,
                                        false));
    }

    private static Long extractHostUptimeSeconds(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        Object raw = metadata.get("hostUptimeSeconds");
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number n) {
            return n.longValue();
        }
        if (raw instanceof String s) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
