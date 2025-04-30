package com.marketingproject.dtos.request;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.marketingproject.infra.exceptions.BusinessRuleException;
import com.marketingproject.shared.constants.SharedConstants;
import com.marketingproject.shared.constants.valitation.ContactValidationMessages;
import com.marketingproject.shared.constants.valitation.OwnerValidationMessages;
import com.marketingproject.shared.utils.TrimStringDeserializer;
import com.marketingproject.shared.utils.ValidateDataUtils;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
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
public class OwnerRequestDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -3963846843873646628L;

    @NotEmpty(message = OwnerValidationMessages.IDENTIFICATION_NUMBER_REQUIRED)
//    @Pattern(regexp = SharedConstants.REGEX_IDENTIFICATION_NUMBER_OWNER, message = OwnerValidationMessages.INVALID_SSN)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String identificationNumber;

    @NotEmpty(message = OwnerValidationMessages.FIRST_NAME_REQUIRED)
    @Size(max = 50, message = OwnerValidationMessages.FIRST_NAME_SIZE)
    @Pattern(regexp = SharedConstants.REGEX_ONLY_LETTERS, message = OwnerValidationMessages.FIRST_NAME_REGEX)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String firstName;

    @Size(max = 150, message = OwnerValidationMessages.LAST_NAME_SIZE)
    @Pattern(regexp = SharedConstants.REGEX_ONLY_LETTERS, message = OwnerValidationMessages.LAST_NAME_REGEX)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String lastName;

    @Size(min = 10, max = 11, message = ContactValidationMessages.PHONE_SIZE)
    @Pattern(regexp = SharedConstants.REGEX_ONLY_NUMBERS, message = ContactValidationMessages.PHONE_ONLY_NUMBERS)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String phone;

    @Email(message = ContactValidationMessages.EMAIL_INVALID)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String email;

    public void validate() {
        if (ValidateDataUtils.isNullOrEmptyString(phone) && ValidateDataUtils.isNullOrEmptyString(email)) {
            throw new BusinessRuleException(OwnerValidationMessages.CONTACT_REQUIRED);
        }
    }
}