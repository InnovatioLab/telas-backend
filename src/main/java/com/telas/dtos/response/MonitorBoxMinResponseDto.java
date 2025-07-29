package com.telas.dtos.response;

import com.telas.entities.Monitor;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
@AllArgsConstructor
public final class MonitorBoxMinResponseDto implements Serializable {
  @Serial
  private static final long serialVersionUID = -2929124221854520175L;

  private final UUID id;
  private final String fullAddress;
  private final boolean hasBox;

  public MonitorBoxMinResponseDto(Monitor entity) {
    id = entity.getId();
    fullAddress = entity.getAddress() != null ? entity.getAddress().getCoordinatesParams() : null;
    hasBox = entity.getBox() != null;
  }
}