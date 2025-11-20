package com.telas.dtos.response;

import com.telas.entities.CartItem;
import com.telas.entities.Monitor;
import com.telas.shared.constants.SharedConstants;
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
    private final String photoUrl;

    public MonitorMapsResponseDto(Monitor entity) {
        id = entity.getId();
        active = entity.isActive();
        latitude = entity.getAddress() != null ? entity.getAddress().getLatitude() : null;
        longitude = entity.getAddress() != null ? entity.getAddress().getLongitude() : null;
        CartItem cartItem = new CartItem();
        cartItem.setBlockQuantity(SharedConstants.MIN_QUANTITY_MONITOR_BLOCK);
        hasAvailableSlots = entity.hasAvailableBlocks(cartItem);
        estimatedSlotReleaseDate = entity.hasAvailableBlocks(cartItem) ? null : entity.getEstimatedSlotReleaseDate();
        adsDailyDisplayTimeInMinutes = entity.getAdsDailyDisplayTimeInMinutes();
        addressLocationName = entity.getAddress() != null ? entity.getAddress().getLocationName() : null;
        addressLocationDescription = entity.getAddress() != null ? entity.getAddress().getLocationDescription() : null;
        photoUrl = entity.getAddress() != null && entity.getAddress().getPhotoUrl() != null ? entity.getAddress().getPhotoUrl() : null;
    }
}