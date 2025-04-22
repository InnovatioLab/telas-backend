package com.marketingproject.dtos.request;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.marketingproject.enums.ContactPreference;
import com.marketingproject.infra.exceptions.BusinessRuleException;
import com.marketingproject.shared.constants.SharedConstants;
import com.marketingproject.shared.constants.valitation.ContactValidationMessages;
import com.marketingproject.shared.utils.TrimStringDeserializer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class ContactRequestDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -3963846843873646628L;

    @NotNull(message = ContactValidationMessages.CONTACT_PREFERENCE_REQUIRED)
    private ContactPreference contactPreference;

    @Pattern(regexp = SharedConstants.REGEX_ONLY_NUMBERS, message = ContactValidationMessages.PHONE_ONLY_NUMBERS)
    @Size(min = 10, max = 11, message = ContactValidationMessages.PHONE_SIZE)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String phone;

    @Email(message = ContactValidationMessages.EMAIL_INVALID)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String email;

    public void validate(boolean emailRequired) {
        validateContactPreference();

        if (emailRequired) {
            validateEmail();
        }
    }

    public void validate() {
        validateContactPreference();
    }

    private void validateContactPreference() {
        if (ContactPreference.PHONE.equals(contactPreference) && phone == null) {
            throw new BusinessRuleException(ContactValidationMessages.PHONE_REQUIRED);
        }

        if (ContactPreference.EMAIL.equals(contactPreference) && email == null) {
            throw new BusinessRuleException(ContactValidationMessages.EMAIL_REQUIRED);
        }
    }

    private void validateEmail() {
        if (email == null || email.isEmpty()) {
            throw new BusinessRuleException(ContactValidationMessages.EMAIL_REQUIRED);
        }
    }
}