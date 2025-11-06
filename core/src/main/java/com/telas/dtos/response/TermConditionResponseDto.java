package com.telas.dtos.response;

import com.telas.entities.TermCondition;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Getter
public final class TermConditionResponseDto implements Serializable {
  @Serial
  private static final long serialVersionUID = 5288515525105234502L;

  private final UUID id;

  private final String version;

  private final String content;

  private final Instant createdAt;

  private final Instant updatedAt;


  public TermConditionResponseDto(TermCondition entity) {
    this.id = entity.getId();
    this.version = entity.getVersion();
    this.content = entity.getContent();
    this.createdAt = entity.getCreatedAt();
    this.updatedAt = entity.getUpdatedAt();
  }
}
