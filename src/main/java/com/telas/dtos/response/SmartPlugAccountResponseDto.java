package com.telas.dtos.response;

import com.telas.monitoring.entities.SmartPlugAccountEntity;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Getter
public final class SmartPlugAccountResponseDto implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private final UUID id;
    private final UUID boxId;
    private final String vendor;
    private final String accountEmail;
    private final boolean passwordConfigured;
    private final boolean enabled;
    private final Instant createdAt;
    private final Instant updatedAt;

    public SmartPlugAccountResponseDto(SmartPlugAccountEntity entity) {
        this.id = entity.getId();
        this.boxId = entity.getBox().getId();
        this.vendor = entity.getVendor();
        this.accountEmail = entity.getAccountEmail();
        this.passwordConfigured =
                entity.getPasswordCipher() != null && !entity.getPasswordCipher().isBlank();
        this.enabled = entity.isEnabled();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
    }
}
