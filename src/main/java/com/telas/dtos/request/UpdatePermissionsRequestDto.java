package com.telas.dtos.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class UpdatePermissionsRequestDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private List<String> permissions;
}
