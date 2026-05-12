package com.telas.dtos.response;

import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Getter
public final class ClientReferenceAttachmentAdminDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 5288515525105234502L;

    private final UUID attachmentId;

    private final Integer businessQuestionnaireVersion;

    private final Instant businessQuestionnaireUpdatedAt;

    private final String previewLink;

    private final String downloadLink;

    public ClientReferenceAttachmentAdminDto(
            UUID attachmentId,
            Integer businessQuestionnaireVersion,
            Instant businessQuestionnaireUpdatedAt,
            String previewLink,
            String downloadLink) {
        this.attachmentId = attachmentId;
        this.businessQuestionnaireVersion = businessQuestionnaireVersion;
        this.businessQuestionnaireUpdatedAt = businessQuestionnaireUpdatedAt;
        this.previewLink = previewLink;
        this.downloadLink = downloadLink;
    }
}
