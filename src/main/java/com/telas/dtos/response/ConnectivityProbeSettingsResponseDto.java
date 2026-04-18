package com.telas.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectivityProbeSettingsResponseDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private long intervalMs;
}
