package com.telas.dtos.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.shared.constants.valitation.ClientValidationMessages;
import com.telas.shared.utils.TrimStringDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SocialMediaRequestDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -3963846843873646628L;

    @URL(message = "Invalid Instagram URL")
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String instagramUrl;

    @URL(message = "Invalid Facebook URL")
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String facebookUrl;

    @URL(message = "Invalid LinkedIn URL")
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String linkedinUrl;

    @URL(message = "Invalid X URL")
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String xUrl;

    @URL(message = "Invalid Tiktok URL")
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String tiktokUrl;

    public void validate() {
        if (instagramUrl == null && facebookUrl == null && linkedinUrl == null && xUrl == null && tiktokUrl == null) {
            throw new BusinessRuleException(ClientValidationMessages.SOCIAL_MEDIA_REQUIRED);
        }
    }
}