package com.telas.dtos.response;

import com.telas.entities.AdMessage;
import com.telas.enums.Role;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Getter
public final class AdMessageResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -8501075279442157348L;

    private final UUID id;
    private final Role senderRole;
    private final String senderName;
    private final String message;
    private final Instant createdAt;

    public AdMessageResponseDto(AdMessage entity) {
        id = entity.getId();
        senderRole = entity.getSenderRole();
        senderName = entity.getUsernameCreate();
        message = entity.getMessage();
        createdAt = entity.getCreatedAt();
    }
}

