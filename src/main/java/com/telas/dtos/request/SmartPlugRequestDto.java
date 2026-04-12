package com.telas.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class SmartPlugRequestDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull private UUID monitorId;

    @NotBlank
    @Size(max = 32)
    private String macAddress;

    @NotBlank
    @Pattern(regexp = "^(KASA|TAPO|TPLINK)$")
    private String vendor;

    @Size(max = 128)
    private String model;

    @Size(max = 255)
    private String displayName;

    @Size(max = 255)
    private String accountEmail;

    @Size(max = 512)
    private String password;

    private Boolean enabled = Boolean.TRUE;

    @Size(max = 45)
    private String lastSeenIp;
}
