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

  private String clientRole;

  private UUID clientId;

  private Instant subscriptionEndsAt;

  private Long subscriptionDaysLeft;

  private String validation;

  private Instant onAirSince;

  private String deploymentStatus;

  private Boolean canRequestRemoval;

  private Boolean partnerRemovalRequested;

  public MonitorAdResponseDto(MonitorAd entity, String adLink) {
    id = entity.getAd().getId();
    orderIndex = entity.getOrderIndex();
    link = adLink;
    fileName = entity.getAd().getName();
    blockQuantity = entity.getBlockQuantity();
    if (entity.getAd().getClient() != null) {
      clientName = entity.getAd().getClient().getBusinessName();
      clientId = entity.getAd().getClient().getId();
      clientRole = entity.getAd().getClient().getRole() != null ? entity.getAd().getClient().getRole().name() : null;
    }
  }

  public void setSubscriptionEndsAt(Instant endsAt) {
    subscriptionEndsAt = endsAt;
    subscriptionDaysLeft = (endsAt != null) ? Duration.between(Instant.now(), endsAt).toDays() : null;
  }
}
