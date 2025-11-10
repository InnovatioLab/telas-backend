package com.telas.dtos.response;

import com.telas.entities.Monitor;
import com.telas.enums.SubscriptionStatus;
import com.telas.shared.constants.SharedConstants;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public final class MonitorWishlistResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -2929124221854520175L;

    private final UUID id;
    private final boolean active;
    private final String addressLocationName;
    private final String addressLocationDescription;
    private final String photoUrl;
    private final String fullAddress;
    private final Double latitude;
    private final Double longitude;
    private final boolean hasAvailableSlots;
    private final LocalDateTime estimatedSlotReleaseDate;


    public MonitorWishlistResponseDto(Monitor entity) {
        id = entity.getId();
        active = entity.isActive() && entity.isAbleToSendBoxRequest();
        addressLocationName = entity.getAddress().getLocationName();
        addressLocationDescription = entity.getAddress().getLocationDescription();
        photoUrl = entity.getAddress().getPhotoUrl();
        fullAddress = entity.getAddress().getCoordinatesParams();
        latitude = entity.getAddress().getLatitude();
        longitude = entity.getAddress().getLongitude();
        hasAvailableSlots = entity.hasAvailableBlocks(SharedConstants.MIN_QUANTITY_MONITOR_BLOCK);
        estimatedSlotReleaseDate = entity.getSubscriptions().stream()
                .filter(subscription -> subscription.getEndsAt() != null && SubscriptionStatus.ACTIVE.equals(subscription.getStatus()))
                .map(subscription -> subscription.getEndsAt().atZone(java.time.ZoneId.of(SharedConstants.ZONE_ID)).toLocalDateTime())
                .min(LocalDateTime::compareTo)
                .orElse(null);
    }
}