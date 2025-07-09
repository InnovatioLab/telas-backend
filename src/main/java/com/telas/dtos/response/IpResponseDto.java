package com.telas.dtos.response;

import com.telas.entities.Ip;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
public final class IpResponseDto implements Serializable {
  @Serial
  private static final long serialVersionUID = 5288515525105234502L;

  private final UUID id;

  private final String ipAddress;

  public IpResponseDto(Ip entity) {
    id = entity.getId();
    ipAddress = entity.getIpAddress();
  }
}
