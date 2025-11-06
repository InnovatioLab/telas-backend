package com.telas.dtos.request;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.AdValidationMessages;
import com.telas.shared.constants.valitation.ContactValidationMessages;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ClientAdRequestToAdminDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 2132868975487514316L;

    private List<UUID> attachmentIds = new ArrayList<>();

    @NotEmpty(message = AdValidationMessages.MESSAGE_REQUIRED)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String message;

    @Pattern(regexp = SharedConstants.REGEX_ONLY_NUMBERS, message = ContactValidationMessages.PHONE_ONLY_NUMBERS)
    @Size(min = 10, max = 10, message = ContactValidationMessages.PHONE_SIZE)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String phone;

    @Email(message = ContactValidationMessages.EMAIL_INVALID)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String email;
}