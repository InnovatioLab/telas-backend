package com.telas.dtos.request;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.telas.dtos.validation.ValidSSN;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.ContactValidationMessages;
import com.telas.shared.constants.valitation.OwnerValidationMessages;
import com.telas.shared.utils.TrimStringDeserializer;
import com.telas.shared.utils.ValidateDataUtils;
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

    @ValidSSN
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