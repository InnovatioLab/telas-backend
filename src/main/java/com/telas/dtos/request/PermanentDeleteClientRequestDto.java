package com.telas.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class PermanentDeleteClientRequestDto {

    @NotBlank
    private String password;

    private UUID monitorSuccessorClientId;
}
