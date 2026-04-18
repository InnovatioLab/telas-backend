package com.telas.dtos.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class UpdateConnectivityProbeSettingsRequestDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    @Min(5000)
    @Max(86400000)
    private Long intervalMs;
}
