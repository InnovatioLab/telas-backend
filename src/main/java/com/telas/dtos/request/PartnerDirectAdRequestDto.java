package com.telas.dtos.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.shared.constants.valitation.AttachmentValidationMessages;
import com.telas.shared.utils.TrimStringDeserializer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
public class PartnerDirectAdRequestDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    @Valid
    private AttachmentRequestDto attachment;

    @Size(max = 255, message = AttachmentValidationMessages.NAME_SIZE)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String adLabel;

    public void validate() {
        attachment.validate();
    }

    public void validateForeignPlacement() {
        validate();
    }
}
