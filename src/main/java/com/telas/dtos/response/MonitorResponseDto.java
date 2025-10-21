package com.telas.dtos.response;

import com.telas.entities.Monitor;
import com.telas.enums.MonitorType;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
public final class MonitorResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -2929124221854520175L;

    private final UUID id;
    private final boolean active;
    private final MonitorType type;
    private final String locationDescription;
    private final AddressFromZipCodeResponseDto address;
    private final String fullAddress;
    private final boolean canBeDeleted;
    private final List<MonitorAdResponseDto> adLinks;

    public MonitorResponseDto(Monitor entity, List<MonitorAdResponseDto> adLinks) {
        id = entity.getId();
        active = entity.isActive();
        type = entity.getType();
        locationDescription = entity.getLocationDescription();
        address = new AddressFromZipCodeResponseDto(entity.getAddress());
        fullAddress = entity.getAddress().getCoordinatesParams();
        canBeDeleted = entity.getActiveSubscriptions().isEmpty();
        this.adLinks = adLinks;
    }
}