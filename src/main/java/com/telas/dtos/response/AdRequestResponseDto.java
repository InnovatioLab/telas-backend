package com.telas.dtos.response;

import com.telas.entities.AdRequest;
import com.telas.enums.Role;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Getter
public final class AdRequestResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 5288515525105234502L;

    private final UUID id;

    private final String message;

    private final String clientName;

    private final String clientIdentificationNumber;

    private final Role clientRole;

    private final String phone;

    private final String email;

    private final boolean isActive;

    private final LocalDate submissionDate;

    private final long waitingDays;

    private final List<LinkResponseDto> attachments;

    public AdRequestResponseDto(AdRequest adRequest, List<LinkResponseDto> attachments) {
        id = adRequest.getId();
        message = adRequest.getMessage();
        clientName = adRequest.getClient().getBusinessName();
        clientIdentificationNumber = adRequest.getClient().getIdentificationNumber();
        clientRole = adRequest.getClient().getRole();
        phone = adRequest.getPhone();
        email = adRequest.getEmail();
        isActive = adRequest.isActive();
        submissionDate = adRequest.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
        waitingDays = ChronoUnit.DAYS.between(adRequest.getCreatedAt(), Instant.now());
        this.attachments = attachments;
    }
}
