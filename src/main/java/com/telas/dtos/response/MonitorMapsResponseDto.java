package com.telas.dtos.response;

import com.telas.entities.Address;
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
    private final Boolean boxActive;

    public MonitorMapsResponseDto(Monitor entity) {
        this(entity, entity.getAdsDailyDisplayTimeInMinutes());
    }

    public MonitorMapsResponseDto(Monitor entity, Integer adsDailyDisplayTimeInMinutes) {
        id = entity.getId();
        active = entity.isActive();
        CartItem cartItem = new CartItem();
        cartItem.setBlockQuantity(SharedConstants.MIN_QUANTITY_MONITOR_BLOCK);
        Address address = entity.getAddress();
        latitude = address != null ? address.getLatitude() : null;
        longitude = address != null ? address.getLongitude() : null;
        hasAvailableSlots = entity.hasAvailableBlocks(cartItem);
        estimatedSlotReleaseDate = entity.hasAvailableBlocks(cartItem) ? null : entity.getEstimatedSlotReleaseDate();
        this.adsDailyDisplayTimeInMinutes = adsDailyDisplayTimeInMinutes;
        if (address != null) {
            addressLocationName = address.resolveMapLocationName();
            addressLocationDescription = address.resolveMapLocationDescription();
            photoUrl = address.getPhotoUrl();
        } else {
            addressLocationName = null;
            addressLocationDescription = null;
            photoUrl = null;
        }
        boxActive = entity.getBox() != null ? entity.getBox().isActive() : null;
    }
}