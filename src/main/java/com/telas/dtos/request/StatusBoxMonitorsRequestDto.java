package com.telas.dtos.request;

import com.telas.enums.DefaultStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StatusBoxMonitorsRequestDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 5288515525105234502L;

    @NotBlank(message = "Field 'ip' is required")
    private String ip;

    private DefaultStatus status;
}
