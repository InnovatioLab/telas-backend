package com.telas.dtos.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class SmartPlugOverviewResponseDto {
    private final UUID id;
    private final String macAddress;
    private final String vendor;
    private final String model;
    private final String displayName;
    private final UUID monitorId;
    private final String monitorAddressSummary;
    private final UUID boxId;
    private final String boxIp;
    private final boolean enabled;
    private final String lastSeenIp;
    private final String accountEmail;
    private final boolean passwordConfigured;
    private final Instant lastReadingAt;
    private final boolean reachable;
    private final Boolean relayOn;
    private final Double powerWatts;
    private final Double voltageVolts;
    private final Double currentAmperes;
    private final String errorCode;
}

