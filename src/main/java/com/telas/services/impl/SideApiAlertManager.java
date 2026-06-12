package com.telas.services.impl;

import com.telas.services.ApplicationLogService;
import com.telas.services.DeveloperNotificationService;
import com.telas.services.SideApiHealthCheckService;
import com.telas.shared.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class SideApiAlertManager {

    private final SideApiHealthCheckService sideApiHealthCheckService;
    private final DeveloperNotificationService developerNotificationService;
    private final ApplicationLogService applicationLogService;

    private final Map<UUID, SideApiAlertState> states = new ConcurrentHashMap<>();

    @Value("${monitoring.sideapi.enabled:true}")
    private boolean sideApiEnabled;

    @Value("${monitoring.sideapi.port:8099}")
    private int sideApiPort;

    @Value("${monitoring.sideapi.path:/health}")
    private String sideApiPath;

    @Value("${monitoring.sideapi.alert.cooldown-ms:600000}")
    private long sideApiAlertCooldownMs;

    public void checkAndAlert(UUID boxId, String ip, Instant now) {
        if (!sideApiEnabled || ip == null || ip.isBlank()) {
            return;
        }

        SideApiHealthCheckService.SideApiHealthOutcome outcome = sideApiHealthCheckService.check(ip);
        SideApiAlertState prev = states.getOrDefault(boxId, SideApiAlertState.initial());
        String url = "http://" + ip + ":" + sideApiPort + normalizePath(sideApiPath);
        String notifiedAt = DateUtils.formatInstantToUsDateTime(now);
        boolean prevDown = Boolean.FALSE.equals(prev.lastUp());
        boolean isInitial = prev.lastUp() == null;

        if (!outcome.up()) {
            Instant downSinceAt = prevDown ? prev.downSinceAt() : now;
            SideApiAlertState next = prev.withLatest(false, now, downSinceAt);

            boolean cooldownOk = prev.lastAlertAt() == null
                    || Duration.between(prev.lastAlertAt(), now).toMillis() >= sideApiAlertCooldownMs;

            if ((isInitial || Boolean.TRUE.equals(prev.lastUp())) && cooldownOk) {
                String detail = outcome.detail() != null ? outcome.detail() : "DOWN";
                Map<String, Object> meta = new HashMap<>();
                meta.put("boxId", boxId.toString());
                meta.put("boxIp", ip);
                meta.put("sideApiUrl", url);
                meta.put("detail", detail);
                if (outcome.httpStatus() != null) {
                    meta.put("httpStatus", outcome.httpStatus());
                }
                applicationLogService.persistSystemLog(
                        "WARN",
                        String.format("SIDE_API: box %s side API DOWN (%s)", ip, detail),
                        "MONITORING",
                        meta
                );
                Map<String, String> params = new HashMap<>();
                params.put("boxIp", ip);
                params.put("sideApiUrl", url);
                params.put("detail", detail);
                params.put("notifiedAt", notifiedAt);
                developerNotificationService.notifyDevelopers(com.telas.enums.NotificationReference.SIDE_API_DOWN, params);
                next = next.withAlertAt(now);
            }
            states.put(boxId, next);
            return;
        }

        if (prevDown) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("boxId", boxId.toString());
            meta.put("boxIp", ip);
            meta.put("sideApiUrl", url);
            applicationLogService.persistSystemLog(
                    "INFO",
                    String.format("SIDE_API: box %s side API reactivated.", ip),
                    "MONITORING",
                    meta
            );
            Map<String, String> params = new HashMap<>();
            params.put("boxIp", ip);
            params.put("sideApiUrl", url);
            params.put("notifiedAt", notifiedAt);
            if (prev.downSinceAt() != null) {
                String downtime = DateUtils.formatDurationHuman(Duration.between(prev.downSinceAt(), now));
                if (downtime != null && !downtime.isBlank()) {
                    params.put("downtime", downtime);
                }
            }
            developerNotificationService.notifyDevelopers(com.telas.enums.NotificationReference.SIDE_API_UP, params);
        }

        states.put(boxId, prev.withLatest(true, now, null));
    }

    private static String normalizePath(String p) {
        if (p == null || p.trim().isEmpty()) {
            return "/health";
        }
        String trimmed = p.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    private record SideApiAlertState(Boolean lastUp, Instant lastCheckedAt, Instant lastAlertAt, Instant downSinceAt) {
        static SideApiAlertState initial() {
            return new SideApiAlertState(null, null, null, null);
        }

        SideApiAlertState withLatest(boolean up, Instant checkedAt, Instant downSinceAt) {
            return new SideApiAlertState(up, checkedAt, lastAlertAt(), downSinceAt);
        }

        SideApiAlertState withAlertAt(Instant alertAt) {
            return new SideApiAlertState(lastUp(), lastCheckedAt(), alertAt, downSinceAt());
        }
    }
}
