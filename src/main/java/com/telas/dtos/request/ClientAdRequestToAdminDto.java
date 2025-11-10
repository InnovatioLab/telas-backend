package com.telas.dtos.request;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.telas.dtos.validation.ValidUrl;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.AdValidationMessages;
import com.telas.shared.constants.valitation.ClientValidationMessages;
import com.telas.shared.constants.valitation.ContactValidationMessages;
import com.telas.shared.utils.TrimStringDeserializer;
import jakarta.validation.Valid;
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

    @Size(max = 50, message = "Slogan must be less than 50 characters.")
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String slogan;

    @ValidUrl(message = "Invalid Brand Guideline URL format")
    @Size(max = 255, message = "Brand Guideline URL must be less than 255 characters.")
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String brandGuidelineUrl;
}