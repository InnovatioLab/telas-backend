package com.telas.dtos.response;

import com.telas.monitoring.plug.PlugReading;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

@Getter
public final class SmartPlugReadingResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final boolean reachable;
    private final Boolean relayOn;
    private final Double powerWatts;
    private final Double voltageVolts;
    private final Double currentAmperes;
    private final String errorCode;

    public SmartPlugReadingResponseDto(PlugReading reading) {
        reachable = reading.reachable();
        relayOn = reading.relayOn();
        powerWatts = reading.powerWatts();
        voltageVolts = reading.voltageVolts();
        currentAmperes = reading.currentAmperes();
        errorCode = reading.errorCode();
    }
}
