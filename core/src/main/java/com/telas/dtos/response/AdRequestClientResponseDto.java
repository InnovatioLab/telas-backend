package com.telas.dtos.response;

import com.telas.entities.AdRequest;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Getter
public final class AdRequestClientResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 5288515525105234502L;

    private final UUID id;

    private final String message;

    private final String phone;

    private final String email;

    private final boolean isActive;

    private final List<UUID> attachmentsIds;

    public AdRequestClientResponseDto(AdRequest adRequest) {
        id = adRequest.getId();
        message = adRequest.getMessage();
        phone = adRequest.getPhone();
        email = adRequest.getEmail();
        isActive = adRequest.isActive();
        attachmentsIds = adRequest.getAttachmentIds() != null
                ? Stream.of(adRequest.getAttachmentIds().split(","))
                .map(UUID::fromString)
                .toList()
                : List.of();


    }
}
