package com.telas.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@AllArgsConstructor
public class PartnerPlatformSettingsResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final boolean partnerSlotsAnyLocationEnabled;
}
