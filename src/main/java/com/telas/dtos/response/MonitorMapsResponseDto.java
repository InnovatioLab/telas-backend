package com.telas.dtos.response;

import com.telas.entities.Monitor;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Getter
public final class MonitorMapsResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -2929124221854520175L;

    private final UUID id;
    private final Boolean active;
    private final Double latitude;
    private final Double longitude;
    private final Boolean hasAvailableSlots;
    private final Instant estimatedSlotReleaseDate;
    private final Integer adsDailyDisplayTimeInMinutes;
    private final String addressLocationName;
    private final String addressLocationDescription;
    private final String monitorLocationDescription;
    private final String photoUrl;

    public MonitorMapsResponseDto(Monitor entity) {
        id = entity.getId();
        active = entity.isActive();
        latitude = entity.getAddress() != null ? entity.getAddress().getLatitude() : null;
        longitude = entity.getAddress() != null ? entity.getAddress().getLongitude() : null;
        hasAvailableSlots = entity.hasAvailableBlocks(1);
        estimatedSlotReleaseDate = entity.getEstimatedSlotReleaseDate();
        adsDailyDisplayTimeInMinutes = entity.getAdsDailyDisplayTimeInMinutes();
        addressLocationName = entity.getAddress() != null ? entity.getAddress().getLocationName() : null;
        addressLocationDescription = entity.getAddress() != null ? entity.getAddress().getLocationDescription() : null;
        monitorLocationDescription = entity.getLocationDescription() != null ? entity.getLocationDescription() : null;
        photoUrl = entity.getAddress() != null && entity.getAddress().getPhotoUrl() != null ? entity.getAddress().getPhotoUrl() : null;
    }
}