package com.telas.dtos.response;

import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Getter
public final class MonitorMapsResponseDto implements Serializable {
  @Serial
  private static final long serialVersionUID = -2929124221854520175L;

  private final String id;
  private final boolean active;
  private final String type;
  private final double size;
  private final double distanceInKm;
  private final double latitude;
  private final double longitude;
  private final boolean hasAvailableSlots;
  private final Instant estimatedSlotReleaseDate;
  private final Integer adsDailyDisplayTimeInMinutes;

  public MonitorMapsResponseDto(
          String id,
          boolean active,
          String type,
          double size,
          double distance,
          double latitude,
          double longitude,
          boolean hasAvailableSlots,
          Instant estimatedSlotReleaseDate,
          Integer adsDailyDisplayTimeInMinutes
  ) {
    this.id = id;
    this.active = active;
    this.type = type;
    this.size = size;
    distanceInKm = distance;
    this.latitude = latitude;
    this.longitude = longitude;
    this.hasAvailableSlots = hasAvailableSlots;
    this.estimatedSlotReleaseDate = hasAvailableSlots ? null : estimatedSlotReleaseDate;
    this.adsDailyDisplayTimeInMinutes = adsDailyDisplayTimeInMinutes;
  }
}