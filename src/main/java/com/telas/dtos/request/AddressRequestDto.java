package com.telas.dtos.request;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.AddressValidationMessages;
import com.telas.shared.utils.TrimStringDeserializer;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddressRequestDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -3963846843873646628L;

    private UUID id;

    @NotEmpty(message = AddressValidationMessages.STREET_REQUIRED)
    @Size(max = 100, message = AddressValidationMessages.STREET_SIZE)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String street;

    @NotEmpty(message = AddressValidationMessages.ZIP_CODE_REQUIRED)
    @Size(min = 5, max = 5, message = AddressValidationMessages.ZIP_CODE_SIZE)
    @Pattern(regexp = SharedConstants.REGEX_ONLY_NUMBERS, message = AddressValidationMessages.ZIP_CODE_REGEX)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String zipCode;

    @NotEmpty(message = AddressValidationMessages.CITY_REQUIRED)
    @Size(max = 50, message = AddressValidationMessages.CITY_SIZE)
    @Pattern(regexp = SharedConstants.REGEX_ALPHANUMERIC, message = AddressValidationMessages.CITY_REGEX)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String city;

    @NotEmpty(message = AddressValidationMessages.STATE_REQUIRED)
    @Size(min = 2, max = 2, message = AddressValidationMessages.STATE_SIZE)
    @Pattern(regexp = SharedConstants.REGEX_ONLY_LETTERS, message = AddressValidationMessages.STATE_REGEX)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String state;

    @Size(max = 50, message = AddressValidationMessages.COUNTRY_SIZE)
    @Pattern(regexp = SharedConstants.REGEX_ONLY_LETTERS, message = AddressValidationMessages.COUNTRY_REGEX)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String country;

    @Size(max = 100, message = AddressValidationMessages.ADDRESS2_SIZE)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String address2;

    private Double latitude;

    private Double longitude;
}