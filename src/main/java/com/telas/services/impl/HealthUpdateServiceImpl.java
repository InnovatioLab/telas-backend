package com.telas.services.impl;

import com.telas.dtos.request.StatusBoxMonitorsRequestDto;
import com.telas.entities.Box;
import com.telas.entities.Monitor;
import com.telas.enums.DefaultStatus;
import com.telas.enums.NotificationReference;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.monitoring.entities.IncidentEntity;
import com.telas.monitoring.repositories.IncidentEntityRepository;
import com.telas.repositories.BoxRepository;
import com.telas.repositories.ClientRepository;
import com.telas.repositories.MonitorRepository;
import com.telas.services.HealthUpdateService;
import com.telas.services.NotificationService;
import com.telas.shared.constants.valitation.BoxValidationMessages;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.shared.utils.DateUtils;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HealthUpdateServiceImpl implements HealthUpdateService {

    private final BoxRepository boxRepository;
    private final MonitorRepository monitorRepository;
    private final ClientRepository clientRepository;
    private final NotificationService notificationService;
    private final IncidentEntityRepository incidentEntityRepository;

    @Override
    @Transactional
    public void applyHealthUpdate(StatusBoxMonitorsRequestDto request) {
        HealthUpdateContext ctx = HealthUpdateContext.from(request);

        if (!ValidateDataUtils.isNullOrEmptyString(request.getIp())) {
            updateHealthByBoxIp(request.getIp(), ctx, request);
            return;
        }

        if (Objects.nonNull(request.getMonitorId())) {
            updateHealthByMonitorId(request.getMonitorId(), ctx, request);
        }
    }

    private void updateHealthByBoxIp(String ip, HealthUpdateContext ctx, StatusBoxMonitorsRequestDto request) {
        Box box = findByAddress(ip);
        box.setActive(ctx.isActive());
        boxRepository.save(box);

        if (box.getMonitors().isEmpty()) {
            recordIncidentIfNeeded(request, ctx, box, null);
            return;
        }

        syncMonitorsActiveState(box.getMonitors(), ctx.isActive());
        Map<String, String> params = buildBoxStatusNotificationParams(ip, ctx, formatLinkedMonitorAddresses(box));
        notifyAdmins(NotificationReference.BOX_STATUS_UPDATED, params);
        recordIncidentIfNeeded(request, ctx, box, null);
    }

    private void updateHealthByMonitorId(UUID monitorId, HealthUpdateContext ctx, StatusBoxMonitorsRequestDto request) {
        Monitor monitor = findMonitorById(monitorId);
        monitor.setActive(ctx.isActive());
        monitorRepository.save(monitor);
        notifyAdmins(NotificationReference.MONITOR_STATUS_UPDATED, buildMonitorStatusNotificationParams(monitor, ctx));
        recordIncidentIfNeeded(request, ctx, null, monitor);
    }

    private void recordIncidentIfNeeded(StatusBoxMonitorsRequestDto request, HealthUpdateContext ctx, Box box,
                                        Monitor monitor) {
        if (!StringUtils.hasText(request.getIncidentType())) {
            return;
        }
        IncidentEntity incident = new IncidentEntity();
        incident.setIncidentType(request.getIncidentType());
        incident.setSeverity(resolveSeverity(request, ctx));
        incident.setBox(box);
        incident.setMonitor(monitor);
        incident.setOpenedAt(Instant.now());
        Map<String, Object> details = new HashMap<>();
        details.put("statusLabel", ctx.statusLabel());
        details.put("active", ctx.isActive());
        incident.setDetailsJson(details);
        incidentEntityRepository.save(incident);
    }

    private static String resolveSeverity(StatusBoxMonitorsRequestDto request, HealthUpdateContext ctx) {
        if (StringUtils.hasText(request.getIncidentSeverity())) {
            return request.getIncidentSeverity();
        }
        return ctx.isActive() ? "INFO" : "CRITICAL";
    }

    private void syncMonitorsActiveState(List<Monitor> monitors, boolean isActive) {
        monitors.forEach(m -> m.setActive(isActive));
        monitorRepository.saveAll(monitors);
    }

    private String formatLinkedMonitorAddresses(Box box) {
        return box.getMonitors().stream().map(m -> m.getAddress().getCoordinatesParams()).collect(Collectors.joining("; "));
    }

    private Map<String, String> buildBoxStatusNotificationParams(String ip, HealthUpdateContext ctx, String monitorAddresses) {
        Map<String, String> params = new HashMap<>();
        params.put("ip", ip);
        params.put("statusLabel", ctx.statusLabel());
        params.put("monitorAddresses", monitorAddresses);
        params.put("notifiedAt", ctx.notifiedAt());
        return params;
    }

    private Map<String, String> buildMonitorStatusNotificationParams(Monitor monitor, HealthUpdateContext ctx) {
        Map<String, String> params = new HashMap<>();
        params.put("monitorAddress", resolveMonitorCoordinates(monitor));
        params.put("statusLabel", ctx.statusLabel());
        params.put("notifiedAt", ctx.notifiedAt());
        return params;
    }

    private String resolveMonitorCoordinates(Monitor monitor) {
        return monitor.getAddress() != null ? monitor.getAddress().getCoordinatesParams() : "Unknown";
    }

    private void notifyAdmins(NotificationReference reference, Map<String, String> params) {
        clientRepository.findAllAdmins().forEach(admin -> notificationService.save(reference, admin, params, false));
    }

    private Monitor findMonitorById(UUID monitorId) {
        return monitorRepository.findById(monitorId)
                .orElseThrow(() -> new ResourceNotFoundException(MonitorValidationMessages.MONITOR_NOT_FOUND));
    }

    private Box findByAddress(String address) {
        return boxRepository.findByAddress(address)
                .orElseThrow(() -> new ResourceNotFoundException(BoxValidationMessages.BOX_NOT_FOUND));
    }

    private record HealthUpdateContext(boolean isActive, String statusLabel, String notifiedAt) {

        static HealthUpdateContext from(StatusBoxMonitorsRequestDto request) {
            boolean isActive = DefaultStatus.ACTIVE.equals(request.getStatus());
            String statusLabel = isActive ? "reactivated" : "deactivated";
            String notifiedAt = DateUtils.formatInstantToUsDateTime(Instant.now());
            return new HealthUpdateContext(isActive, statusLabel, notifiedAt);
        }
    }
}
