package com.telas.dtos.response;

import com.telas.entities.MonitorAd;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public final class MonitorAdResponseDto implements Serializable {
  @Serial
  private static final long serialVersionUID = -7144327643566339527L;

  private UUID id;

  private String link;

  private String fileName;

  private Integer orderIndex;

  public MonitorAdResponseDto(MonitorAd entity, String adLink) {
    id = entity.getAd().getId();
    orderIndex = entity.getOrderIndex();
    link = adLink;
    fileName = entity.getAd().getName();
  }
}
