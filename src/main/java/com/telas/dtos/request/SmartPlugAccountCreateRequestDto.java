package com.telas.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class SmartPlugAccountCreateRequestDto {

    @NotNull private UUID boxId;

    @NotBlank private String vendor;

    private String accountEmail;

    private String password;

    private Boolean enabled;
}
