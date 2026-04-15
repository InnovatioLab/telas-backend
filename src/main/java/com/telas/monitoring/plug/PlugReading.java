package com.telas.monitoring.plug;

public record PlugReading(
        boolean reachable,
        Boolean relayOn,
        Double powerWatts,
        Double voltageVolts,
        Double currentAmperes,
        String errorCode) {

    public static PlugReading unreachable(String errorCode) {
        return new PlugReading(false, null, null, null, null, errorCode);
    }
}
