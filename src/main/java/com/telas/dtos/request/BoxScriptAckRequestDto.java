package com.telas.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class BoxScriptAckRequestDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull private UUID commandId;

    @NotBlank private String boxAddress;

    private boolean success;

    private String errorMessage;
}
