package com.telas.enums;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
public enum Recurrence {
  THIRTY_DAYS(30, BigDecimal.valueOf(1L)),
  SIXTY_DAYS(60, BigDecimal.valueOf(2L)),
  NINETY_DAYS(90, BigDecimal.valueOf(3L)),
  MONTHLY(0, BigDecimal.valueOf(1L));

  private final long days;
  private final BigDecimal multiplier;

  Recurrence(long days, BigDecimal multiplier) {
    this.days = days;
    this.multiplier = multiplier;
  }

  public Instant calculateEndsAt(Instant startedAt) {
    return days > 0 ? startedAt.plusSeconds(days * 24 * 60 * 60) : null;
  }
}
