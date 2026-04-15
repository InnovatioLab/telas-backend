package com.telas.dtos.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class SmartPlugRequestDto extends SmartPlugMetadataRequestDto {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull private UUID monitorId;
}
