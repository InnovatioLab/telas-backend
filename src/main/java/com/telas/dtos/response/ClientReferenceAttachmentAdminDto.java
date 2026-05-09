package com.telas.dtos.response;

import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
public final class ClientReferenceAttachmentAdminDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 5288515525105234502L;

    private final UUID attachmentId;

    private final String slogan;

    private final String brandGuidelineUrl;

    private final String previewLink;

    private final String downloadLink;

    public ClientReferenceAttachmentAdminDto(
            UUID attachmentId,
            String slogan,
            String brandGuidelineUrl,
            String previewLink,
            String downloadLink) {
        this.attachmentId = attachmentId;
        this.slogan = slogan;
        this.brandGuidelineUrl = brandGuidelineUrl;
        this.previewLink = previewLink;
        this.downloadLink = downloadLink;
    }
}
