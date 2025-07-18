package com.telas.dtos.response;

import com.telas.entities.BoxAddress;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
public final class BoxAddressResponseDto implements Serializable {
  @Serial
  private static final long serialVersionUID = 5288515525105234502L;

  private final UUID id;

  private final String mac;

  private final String ip;

  public BoxAddressResponseDto(BoxAddress entity) {
    id = entity.getId();
    mac = entity.getMac();
    ip = entity.getIp();
  }
}
