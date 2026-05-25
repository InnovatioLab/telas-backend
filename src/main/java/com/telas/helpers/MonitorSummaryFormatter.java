package com.telas.helpers;

import com.telas.entities.Monitor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class MonitorSummaryFormatter {

    public String formatMonitorLine(Monitor monitor) {
        if (monitor == null) {
            return "";
        }
        String addressPart = monitor.getAddress() != null
                ? monitor.getAddress().resolveMapLocationDescription()
                : "";
        String ip = monitor.getBox() != null && monitor.getBox().getBoxAddress() != null
                ? monitor.getBox().getBoxAddress().getIp()
                : "";
        if (addressPart != null && !addressPart.isBlank() && ip != null && !ip.isBlank()) {
            return addressPart + " — Box " + ip;
        }
        if (addressPart != null && !addressPart.isBlank()) {
            return addressPart;
        }
        if (ip != null && !ip.isBlank()) {
            return "Box " + ip;
        }
        return monitor.getId() != null ? monitor.getId().toString() : "";
    }

    public String formatMonitorsSummary(Collection<Monitor> monitors) {
        if (monitors == null || monitors.isEmpty()) {
            return "";
        }
        return monitors.stream()
                .map(this::formatMonitorLine)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining("; "));
    }

    public String formatMonitorsSummary(Collection<Monitor> monitors, Predicate<Monitor> include) {
        if (monitors == null || monitors.isEmpty()) {
            return "";
        }
        return monitors.stream()
                .filter(include)
                .map(this::formatMonitorLine)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining("; "));
    }

    public String formatMonitorsSummaryForAd(
            Collection<Monitor> monitors,
            UUID adId,
            MonitorAdRelevanceChecker relevanceChecker) {
        if (monitors == null || monitors.isEmpty() || adId == null) {
            return "";
        }
        List<String> lines = monitors.stream()
                .filter(monitor -> relevanceChecker.isRelevant(monitor, adId))
                .map(this::formatMonitorLine)
                .filter(s -> s != null && !s.isBlank())
                .toList();
        return String.join("; ", lines);
    }

    @FunctionalInterface
    public interface MonitorAdRelevanceChecker {
        boolean isRelevant(Monitor monitor, UUID adId);
    }
}
