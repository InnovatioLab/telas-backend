package com.telas.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class BoxLogRequestDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    private String level;

    @NotBlank
    private String message;

    private String boxAddress;

    private Map<String, Object> metadata;
}
