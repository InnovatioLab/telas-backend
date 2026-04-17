package com.telas.dtos.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class UpdateEmailAlertPreferencesRequestDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Map<String, Boolean> preferences;
}
