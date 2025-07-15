package com.telas.dtos.response;

import com.telas.entities.Box;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
public final class BoxResponseDto implements Serializable {
  @Serial
  private static final long serialVersionUID = 5288515525105234502L;

  private final UUID id;

  private final boolean active;

  private final BoxAddressResponseDto boxAddress;

  private final List<MonitorMinResponseDto> monitors;

  public BoxResponseDto(Box entity) {
    id = entity.getId();
    active = entity.isActive();
    boxAddress = new BoxAddressResponseDto(entity.getBoxAddress());
    monitors = entity.getMonitors().stream()
            .map(MonitorMinResponseDto::new)
            .toList();
  }
}
