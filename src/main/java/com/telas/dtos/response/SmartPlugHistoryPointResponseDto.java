package com.telas.dtos.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class SmartPlugHistoryPointResponseDto {
    private final Instant at;
    private final boolean reachable;
    private final Boolean relayOn;
    private final Double powerWatts;
    private final Double voltageVolts;
    private final Double currentAmperes;
    private final String errorCode;
}

