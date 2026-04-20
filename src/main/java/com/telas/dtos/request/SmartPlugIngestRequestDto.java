package com.telas.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class SmartPlugIngestRequestDto {

    @NotNull
    private UUID boxId;

    private UUID smartPlugId;

    @NotBlank
    private String macAddress;

    @Pattern(regexp = "^(KASA|TAPO|TPLINK)?$")
    private String vendor;

    private String resolvedIp;

    @NotNull
    private Instant at;

    @NotNull
    private Boolean reachable;

    private Boolean relayOn;
    private Double powerWatts;
    private Double voltageVolts;
    private Double currentAmperes;
    private String errorCode;
}

