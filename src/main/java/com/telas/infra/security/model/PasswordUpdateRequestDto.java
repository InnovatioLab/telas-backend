package com.telas.infra.security.model;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.shared.constants.valitation.ClientValidationMessages;
import com.telas.shared.utils.TrimStringDeserializer;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PasswordUpdateRequestDto extends PasswordRequestDto {
    @Serial
    private static final long serialVersionUID = -5691582085040881211L;

    @NotBlank(message = "Current password is required")
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String currentPassword;

    @Override
    public void validate() {
        if (!getPassword().equals(getConfirmPassword())) {
            throw new BusinessRuleException(ClientValidationMessages.DIFFERENT_PASSWORDS);
        }

        if (getPassword().equals(currentPassword)) {
            throw new BusinessRuleException(ClientValidationMessages.CURRENT_EQUALS_NEW_PASSWORD);
        }
    }


}