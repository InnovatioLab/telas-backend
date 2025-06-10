package com.telas.dtos.response;

import com.telas.entities.MonitorAd;
import com.telas.enums.DisplayType;
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

  private UUID adId;

  private String adLink;

  private DisplayType displayType;

  private Integer orderIndex;

  public MonitorAdResponseDto(MonitorAd entity, String adLink) {
    adId = entity.getAd().getId();
    displayType = entity.getDisplayType();
    orderIndex = entity.getOrderIndex();
    this.adLink = adLink;
  }
}
