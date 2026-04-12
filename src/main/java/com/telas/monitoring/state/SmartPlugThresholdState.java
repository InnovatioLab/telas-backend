package com.telas.monitoring.state;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SmartPlugThresholdState {

    private final ConcurrentHashMap<UUID, Integer> lowPowerStreak = new ConcurrentHashMap<>();

    public int incrementLowPower(UUID plugId) {
        return lowPowerStreak.merge(plugId, 1, Integer::sum);
    }

    public void resetLowPower(UUID plugId) {
        lowPowerStreak.remove(plugId);
    }

    public void clear() {
        lowPowerStreak.clear();
    }
}
