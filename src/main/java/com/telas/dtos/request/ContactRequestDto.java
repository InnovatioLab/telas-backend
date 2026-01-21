package com.telas.dtos.request;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.ContactValidationMessages;
import com.telas.shared.utils.TrimLowercaseDeserializer;
import com.telas.shared.utils.TrimStringDeserializer;
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
public class ContactRequestDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -3963846843873646628L;

    @NotEmpty(message = ContactValidationMessages.PHONE_REQUIRED)
    @Pattern(regexp = SharedConstants.REGEX_ONLY_NUMBERS, message = ContactValidationMessages.PHONE_ONLY_NUMBERS)
    @Size(min = 10, max = 10, message = ContactValidationMessages.PHONE_SIZE)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String phone;

    @NotEmpty(message = ContactValidationMessages.EMAIL_REQUIRED)
    @Email(message = ContactValidationMessages.EMAIL_INVALID)
    @JsonDeserialize(using = TrimLowercaseDeserializer.class)
    private String email;
}