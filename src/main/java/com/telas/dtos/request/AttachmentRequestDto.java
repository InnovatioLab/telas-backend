package com.telas.dtos.request;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.AttachmentValidationMessages;
import com.telas.shared.utils.TrimStringDeserializer;
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
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AttachmentRequestDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -3963846843873646628L;

    private UUID id;

    @NotEmpty(message = AttachmentValidationMessages.NAME_REQUIRED)
    @Pattern(regexp = SharedConstants.REGEX_ATTACHMENT_NAME, message = AttachmentValidationMessages.NAME_INVALID)
    @Size(max = 255, message = AttachmentValidationMessages.NAME_SIZE)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String name;

    @NotEmpty(message = AttachmentValidationMessages.TYPE_REQUIRED)
    @Pattern(regexp = SharedConstants.REGEX_ATTACHMENT_TYPE, message = AttachmentValidationMessages.TYPE_INVALID)
    @Size(max = 15, message = AttachmentValidationMessages.TYPE_SIZE)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String type;

    @NotNull(message = AttachmentValidationMessages.BYTES_REQUIRED)
    private byte[] bytes;

    public void validate() {
        if (bytes.length > SharedConstants.MAX_ATTACHMENT_SIZE) {
            throw new BusinessRuleException(AttachmentValidationMessages.ERROR_UPLOAD);
        }
    }
}