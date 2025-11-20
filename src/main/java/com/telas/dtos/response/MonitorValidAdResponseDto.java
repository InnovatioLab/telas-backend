package com.telas.dtos.response;

import com.telas.entities.Ad;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public final class MonitorValidAdResponseDto implements Serializable {
      @Serial
      private static final long serialVersionUID = -7144327643566339527L;

      private UUID id;

      private String link;

      private String fileName;

      private Boolean isAttachedToMonitor;

      private Integer orderIndex;

    public MonitorValidAdResponseDto(Ad ad, String adLink, Boolean isAttachedToMonitor, Integer orderIndex) {
        id = ad.getId();
        link = adLink;
        fileName = ad.getName();
        this.isAttachedToMonitor = isAttachedToMonitor;
        this.orderIndex = orderIndex;
    }
}
