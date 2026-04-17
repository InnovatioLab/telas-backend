package com.telas.dtos.response;

import lombok.Builder;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Value
@Builder
public class EmailAlertPreferencesResponseDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    Map<String, Boolean> preferences;
}
