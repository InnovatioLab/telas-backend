package com.telas.dtos.response;

import com.telas.entities.AdMessage;
import com.telas.enums.Role;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Getter
public final class AdminClientMessageRowDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 4493753963143969915L;

    private final UUID adId;
    private final String adName;
    private final UUID messageId;
    private final Role senderRole;
    private final String senderName;
    private final String message;
    private final Instant createdAt;

    public AdminClientMessageRowDto(AdMessage entity) {
        adId = entity.getAd() != null ? entity.getAd().getId() : null;
        adName = entity.getAd() != null ? entity.getAd().getName() : null;
        messageId = entity.getId();
        senderRole = entity.getSenderRole();
        senderName = entity.getUsernameCreate();
        message = entity.getMessage();
        createdAt = entity.getCreatedAt();
    }
}

