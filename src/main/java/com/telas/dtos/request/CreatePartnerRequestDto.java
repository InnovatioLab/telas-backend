package com.telas.dtos.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.ClientValidationMessages;
import com.telas.shared.utils.TrimStringDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

@Getter
@Setter
public class CreatePartnerRequestDto extends ClientRequestDto {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = ClientValidationMessages.PASSWORD_REQUIRED)
    @Pattern(regexp = SharedConstants.REGEX_PASSWORD, message = ClientValidationMessages.PASSWORD_REGEX)
    @Size(min = 8, max = 32, message = ClientValidationMessages.PASSWORD_SIZE)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String password;

    @NotBlank(message = ClientValidationMessages.CONFIRM_PASSWORD_REQUIRED)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String confirmPassword;
}
