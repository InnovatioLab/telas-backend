package com.marketingproject.dtos.response;

import com.marketingproject.entities.Address;
import com.marketingproject.entities.Monitor;
import com.marketingproject.enums.MonitorType;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
public final class MonitorResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -2929124221854520175L;

    private final UUID id;
    private final boolean active;
    private final MonitorType type;
    private final BigDecimal size;
    private final Double latitude;
    private final Double longitude;
    private final Address address;
    private final List<LinkResponseDto> advertisingAttachments;

    public MonitorResponseDto(Monitor entity, List<LinkResponseDto> advertisingAttachmentsLinks) {
        id = entity.getId();
        active = entity.isActive();
        type = entity.getType();
        size = entity.getSize();
        latitude = entity.getLatitude();
        longitude = entity.getLongitude();
        address = entity.getAddress();
        advertisingAttachments = advertisingAttachmentsLinks;
    }
}