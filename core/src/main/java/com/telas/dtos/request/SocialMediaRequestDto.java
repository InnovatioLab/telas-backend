package com.telas.dtos.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.telas.dtos.validation.ValidUrl;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.shared.utils.TrimStringDeserializer;
import jakarta.validation.constraints.Size;
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

    @ValidUrl(message = "Invalid Instagram URL")
    @Size(max = 255, message = "Instagram URL size must be up to 255 characters.")
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String instagramUrl;

    @ValidUrl(message = "Invalid Facebook URL")
    @Size(max = 255, message = "Facebook URL size must be up to 255 characters.")
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String facebookUrl;

    @ValidUrl(message = "Invalid LinkedIn URL")
    @Size(max = 255, message = "LinkedIn URL size must be up to 255 characters.")
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String linkedinUrl;

    @ValidUrl(message = "Invalid X URL")
    @Size(max = 255, message = "X URL size must be up to 255 characters.")
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String xUrl;

    @URL(message = "Invalid Tiktok URL")
    @Size(max = 255, message = "TikTok URL size must be up to 255 characters.")
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String tiktokUrl;

    public void validate() {
        if (instagramUrl == null && facebookUrl == null && linkedinUrl == null && xUrl == null && tiktokUrl == null) {
            throw new BusinessRuleException("At least one social media URL is required.");
        }
    }
}
