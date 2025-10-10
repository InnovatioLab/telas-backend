package com.telas.dtos.request;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.telas.dtos.validation.ValidUrl;
import com.telas.enums.DefaultStatus;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.ClientValidationMessages;
import com.telas.shared.utils.TrimStringDeserializer;
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

    @Pattern(regexp = SharedConstants.REGEX_ONLY_LETTERS, message = ClientValidationMessages.INDUSTRY_REGEX)
    @Size(max = 50, message = ClientValidationMessages.INDUSTRY_SIZE)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String industry;

    @ValidUrl(message = "Invalid website URL format")
    @Size(max = 255, message = ClientValidationMessages.WEBSITE_URL_SIZE)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String websiteUrl;

    private DefaultStatus status = DefaultStatus.INACTIVE;

    @NotNull(message = ClientValidationMessages.CONTACT_REQUIRED)
    private @Valid ContactRequestDto contact;

    @NotNull(message = ClientValidationMessages.ADDRESSES_REQUIRED)
    @NotEmpty(message = ClientValidationMessages.ADDRESSES_REQUIRED)
    private @Valid List<AddressRequestDto> addresses;

    private SocialMediaRequestDto socialMedia;

    public void validate() {
        if (socialMedia != null) {
            socialMedia.validate();
        }
    }
}