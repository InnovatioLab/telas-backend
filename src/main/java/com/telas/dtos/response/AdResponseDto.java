package com.telas.dtos.response;

import com.telas.entities.Ad;
import com.telas.enums.AdValidationType;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Getter
public final class AdResponseDto implements Serializable {
  @Serial
  private static final long serialVersionUID = 5288515525105234502L;

  private final UUID id;

  private final String name;

  private final LocalDate submissionDate;

  private final String link;

  private final AdValidationType validation;

  private final long waitingDays;

  public AdResponseDto(Ad ad, String link) {
    id = ad.getId();
    name = ad.getName();
    submissionDate = ad.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
    this.link = link;
    validation = ad.getValidation();
    waitingDays = ChronoUnit.DAYS.between(ad.getCreatedAt(), Instant.now());
  }
}
