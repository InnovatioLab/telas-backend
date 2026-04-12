package com.telas.dtos.response;

import com.telas.monitoring.entities.SmartPlugEntity;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Getter
public final class SmartPlugResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String macAddress;
    private final String vendor;
    private final String model;
    private final String displayName;
    private final UUID monitorId;
    private final boolean enabled;
    private final String lastSeenIp;
    private final String accountEmail;
    private final boolean passwordConfigured;
    private final Instant createdAt;
    private final Instant updatedAt;

    public SmartPlugResponseDto(SmartPlugEntity entity) {
        id = entity.getId();
        macAddress = entity.getMacAddress();
        vendor = entity.getVendor();
        model = entity.getModel();
        displayName = entity.getDisplayName();
        monitorId = entity.getMonitor() != null ? entity.getMonitor().getId() : null;
        enabled = entity.isEnabled();
        lastSeenIp = entity.getLastSeenIp();
        accountEmail = entity.getAccountEmail();
        passwordConfigured = entity.getPasswordCipher() != null && !entity.getPasswordCipher().isEmpty();
        createdAt = entity.getCreatedAt();
        updatedAt = entity.getUpdatedAt();
    }
}
