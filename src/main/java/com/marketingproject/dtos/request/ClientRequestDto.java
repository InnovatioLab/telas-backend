package com.marketingproject.dtos.request;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.marketingproject.enums.DefaultStatus;
import com.marketingproject.enums.Role;
import com.marketingproject.shared.constants.SharedConstants;
import com.marketingproject.shared.constants.valitation.ClientValidationMessages;
import com.marketingproject.shared.constants.valitation.OwnerValidationMessages;
import com.marketingproject.shared.utils.TrimStringDeserializer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ClientRequestDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -3963846843873646628L;

    @NotEmpty(message = ClientValidationMessages.BUSINESS_NAME_REQUIRED)
    @Size(max = 255, message = ClientValidationMessages.BUSINESS_NAME_SIZE)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String businessName;

    @NotEmpty(message = OwnerValidationMessages.IDENTIFICATION_NUMBER_REQUIRED)
    @Size(min = 9, max = 9, message = OwnerValidationMessages.IDENTIFICATION_NUMBER_SIZE)
    @Pattern(regexp = SharedConstants.REGEX_ONLY_NUMBERS, message = OwnerValidationMessages.IDENTIFICATION_NUMBER_REGEX)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String identificationNumber;

    @NotEmpty(message = ClientValidationMessages.BUSINESS_FIELD_REQUIRED)
    @Pattern(regexp = SharedConstants.REGEX_ONLY_LETTERS, message = ClientValidationMessages.BUSINESS_FIELD_REGEX)
    @Size(max = 50, message = ClientValidationMessages.BUSINESS_FIELD_SIZE)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String businessField;

    private Role role = Role.CLIENT;

    private DefaultStatus status = DefaultStatus.INACTIVE;

    @NotNull(message = ClientValidationMessages.CONTACT_REQUIRED)
    private @Valid ContactRequestDto contact;

    @NotNull(message = ClientValidationMessages.OWNER_REQUIRED)
    private @Valid OwnerRequestDto owner;

    @NotNull(message = ClientValidationMessages.ADDRESSES_REQUIRED)
    @NotEmpty(message = ClientValidationMessages.ADDRESSES_REQUIRED)
    private @Valid List<AddressRequestDto> addresses;

    public void validate() {
        contact.validate(true);
    }

}