package com.telas.services;

import java.util.List;

public record RemoveMonitorAdsOutcome(List<String> removedAdNames) {
    public static RemoveMonitorAdsOutcome none() {
        return new RemoveMonitorAdsOutcome(List.of());
    }
}
