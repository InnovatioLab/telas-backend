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
import java.util.Map;
import java.util.UUID;

@Getter
public final class AdRequestAdminResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 5288515525105234502L;

    private final UUID id;

    private final UUID clientId;

    private final String message;

    private final String clientName;

    private final Role clientRole;

    private final String phone;

    private final String email;

    private final boolean isActive;

    private final LocalDate submissionDate;

    private final long waitingDays;

    private final List<RefusedAdResponseDto> refusedAds;

    private final List<LinkResponseDto> attachments;

    private final LinkResponseDto ad;

    public AdRequestAdminResponseDto(AdRequest adRequest, Map<String, Object> linkResponseData) {
        id = adRequest.getId();
        clientId = adRequest.getClient().getId();
        message = adRequest.getMessage();
        clientName = adRequest.getClient().getBusinessName();
        clientRole = adRequest.getClient().getRole();
        phone = adRequest.getPhone();
        email = adRequest.getEmail();
        isActive = adRequest.isActive();
        submissionDate = adRequest.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
        waitingDays = ChronoUnit.DAYS.between(adRequest.getCreatedAt(), Instant.now());
        attachments = (List<LinkResponseDto>) linkResponseData.get("attachments");
        ad = (LinkResponseDto) linkResponseData.get("ad");

        refusedAds = adRequest.getAd() != null && !adRequest.getAd().getRefusedAds().isEmpty() ?
                adRequest.getAd().getRefusedAds().stream()
                        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                        .map(RefusedAdResponseDto::new)
                        .toList() : List.of();
    }
}
