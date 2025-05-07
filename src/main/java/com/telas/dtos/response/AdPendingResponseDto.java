package com.telas.dtos.response;

import com.telas.entities.AdvertisingAttachment;
import com.telas.enums.AttachmentValidationType;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Getter
public final class AttachmentPendingResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 5288515525105234502L;

    private final UUID id;

    private final String name;

    private final String businessName;

    private final LocalDate submissionDate;

    private final String link;

    private final AttachmentValidationType validation;

    private final long waitingDays;

    public AttachmentPendingResponseDto(AdvertisingAttachment attachment, String link) {
        id = attachment.getId();
        name = attachment.getName();
        businessName = attachment.getClient().getBusinessName();
        submissionDate = attachment.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
        this.link = link;
        validation = attachment.getValidation();
        waitingDays = ChronoUnit.DAYS.between(attachment.getCreatedAt(), Instant.now());
    }
}
