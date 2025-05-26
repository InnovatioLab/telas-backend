package com.telas.dtos.response;

import com.telas.entities.RefusedAd;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
public final class RefusedAdResponseDto implements Serializable {
  @Serial
  private static final long serialVersionUID = 5288515525105234502L;

  private final UUID id;

  private final String justification;

  private final String description;

  public RefusedAdResponseDto(RefusedAd entity) {
    id = entity.getId();
    justification = entity.getJustification();
    description = entity.getDescription();
  }
}
