package com.telas.dtos.response;

import com.telas.entities.Subscription;
import com.telas.enums.Recurrence;
import com.telas.enums.SubscriptionStatus;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class SubscriptionResponseDto implements Serializable {
  @Serial
  private static final long serialVersionUID = -2929124221854520175L;

  private UUID id;

  private BigDecimal amount;

  private Recurrence recurrence;

  private boolean bonus;

  private SubscriptionStatus status;

  private Instant startedAt;

  private Instant endsAt;

  private Long daysLeft;

  private List<PaymentResponseDto> payments;

  private List<SubscriptionMonitorResponseDto> monitors;

  public SubscriptionResponseDto(Subscription entity) {
    id = entity.getId();
    amount = entity.getPaidAmount();
    recurrence = entity.getRecurrence();
    bonus = entity.isBonus();
    status = entity.getStatus();
    startedAt = entity.getStartedAt();
    endsAt = entity.getEndsAt();
    daysLeft = (endsAt != null) ? Duration.between(Instant.now(), endsAt).toDays() : null;
    payments = entity.getPayments().stream()
            .map(PaymentResponseDto::new)
            .toList();
    monitors = entity.getMonitors().stream()
            .map(SubscriptionMonitorResponseDto::new)
            .toList();
  }
}