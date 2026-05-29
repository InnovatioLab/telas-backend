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
public class UpdateBoxCarouselSettingsRequestDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    @Min(1000)
    @Max(60000)
    private Integer displayDurationMs;

    @NotNull
    @Min(0)
    @Max(10000)
    private Integer transitionDurationMs;

    @NotNull
    @Min(1)
    @Max(30)
    private Integer totalSlots;
}
