package com.telas.dtos.response;

import com.telas.entities.Monitor;
import com.telas.enums.MonitorType;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public final class MonitorMapsResponseDto implements Serializable {
  @Serial
  private static final long serialVersionUID = -2929124221854520175L;

  private final UUID id;
  private final Boolean active;
  private final MonitorType type;
  private final BigDecimal size;
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
    this.id = entity.getId();
    this.active = entity.isActive();
    this.type = entity.getType();
    this.size = entity.getSize();
    this.latitude = entity.getAddress() != null ? entity.getAddress().getLatitude() : null;
    this.longitude = entity.getAddress() != null ? entity.getAddress().getLongitude() : null;
    this.hasAvailableSlots = entity.hasAvailableBlocks(1);
    this.estimatedSlotReleaseDate = entity.getEstimatedSlotReleaseDate();
    this.adsDailyDisplayTimeInMinutes = entity.getAdsDailyDisplayTimeInMinutes();
    this.addressLocationName = entity.getAddress() != null ? entity.getAddress().getLocationName() : null;
    this.addressLocationDescription = entity.getAddress() != null ? entity.getAddress().getLocationDescription() : null;
    this.monitorLocationDescription = entity.getLocationDescription() != null ? entity.getLocationDescription() : null;
    this.photoUrl = entity.getAddress() != null && entity.getAddress().getPhotoUrl() != null ? entity.getAddress().getPhotoUrl() : null;
  }
}