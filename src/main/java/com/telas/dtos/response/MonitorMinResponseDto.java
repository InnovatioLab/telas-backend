package com.telas.dtos.response;

import com.telas.entities.Monitor;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

@Getter
public final class MonitorMinResponseDto implements Serializable {
  @Serial
  private static final long serialVersionUID = -2929124221854520175L;

  private final String id;
  private final boolean active;
  private final String type;
  private final double size;
  private final double distanceInKm;
  private final double latitude;
  private final double longitude;

  public MonitorMinResponseDto(Monitor monitor, double distance, double latitude, double longitude) {
    id = monitor.getId().toString();
    active = monitor.isActive();
    type = monitor.getType().name();
    size = monitor.getSize().doubleValue();
    distanceInKm = distance;
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public MonitorMinResponseDto(String id, boolean active, String type, double size, double distance, double latitude, double longitude) {
    this.id = id;
    this.active = active;
    this.type = type;
    this.size = size;
    distanceInKm = distance;
    this.latitude = latitude;
    this.longitude = longitude;
  }
}