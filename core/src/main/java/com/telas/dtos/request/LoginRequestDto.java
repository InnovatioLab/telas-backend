package com.telas.dtos.request;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.telas.shared.constants.valitation.ClientValidationMessages;
import com.telas.shared.utils.TrimStringDeserializer;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequestDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -3963846843873646628L;

    @NotBlank(message = "Username is required")
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String username;

    @NotBlank(message = ClientValidationMessages.PASSWORD_REQUIRED)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String password;
}