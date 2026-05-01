package com.telas.dtos.response;

import com.telas.entities.MonitorAd;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
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

  private Integer blockQuantity;

  private String clientName;

  private Instant subscriptionEndsAt;

  private Long subscriptionDaysLeft;

  public MonitorAdResponseDto(MonitorAd entity, String adLink) {
    id = entity.getAd().getId();
    orderIndex = entity.getOrderIndex();
    link = adLink;
    fileName = entity.getAd().getName();
    blockQuantity = entity.getBlockQuantity();
    clientName = entity.getAd().getClient() != null ? entity.getAd().getClient().getBusinessName() : null;
  }

  public void setSubscriptionEndsAt(Instant endsAt) {
    subscriptionEndsAt = endsAt;
    subscriptionDaysLeft = (endsAt != null) ? Duration.between(Instant.now(), endsAt).toDays() : null;
  }
}
