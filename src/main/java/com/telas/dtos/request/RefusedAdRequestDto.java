package com.telas.dtos.request;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.telas.shared.constants.valitation.AttachmentValidationMessages;
import com.telas.shared.utils.TrimStringDeserializer;
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
public class RefusedAdRequestDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 3742070221704178674L;
    @Size(max = 100, message = AttachmentValidationMessages.JUSTIFICATION_LENGTH)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String justification;

    @Size(max = 255, message = AttachmentValidationMessages.DESCRIPTION_LENGTH)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String description;
}