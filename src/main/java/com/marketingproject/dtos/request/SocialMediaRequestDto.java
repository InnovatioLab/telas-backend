package com.marketingproject.dtos.request;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.marketingproject.infra.exceptions.BusinessRuleException;
import com.marketingproject.shared.constants.valitation.ClientValidationMessages;
import com.marketingproject.shared.utils.TrimStringDeserializer;
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
public class SocialMediaRequestDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -3963846843873646628L;

    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String instagramUrl;

    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String facebookUrl;

    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String linkedinUrl;

    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String xUrl;

    public void validate() {
        if (instagramUrl == null && facebookUrl == null && linkedinUrl == null && xUrl == null) {
            throw new BusinessRuleException(ClientValidationMessages.SOCIAL_MEDIA_REQUIRED);
        }
    }

}