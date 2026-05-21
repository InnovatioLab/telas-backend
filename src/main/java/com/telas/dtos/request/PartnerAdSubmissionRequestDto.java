package com.telas.dtos.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.telas.enums.PartnerSubmissionMode;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PartnerAdSubmissionRequestDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private PartnerSubmissionMode submissionMode;

    @Valid
    private AttachmentRequestDto attachment;

    private List<UUID> attachmentIds = new ArrayList<>();

    @Size(max = 255, message = AttachmentValidationMessages.NAME_SIZE)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String optionalLabel;

    public void validate() {
        if (submissionMode == null) {
            throw new BusinessRuleException("Submission mode is required.");
        }
        switch (submissionMode) {
            case READY_CREATIVE -> {
                if (attachment == null) {
                    throw new BusinessRuleException(AttachmentValidationMessages.ATTACHMENT_LIST_EMPTY);
                }
                attachment.validate();
            }
            case ADMIN_MATERIALS -> {
                if (attachmentIds == null || attachmentIds.isEmpty()) {
                    throw new BusinessRuleException(AttachmentValidationMessages.ATTACHMENT_LIST_EMPTY);
                }
            }
            default -> throw new BusinessRuleException("Unsupported submission mode.");
        }
    }
}
