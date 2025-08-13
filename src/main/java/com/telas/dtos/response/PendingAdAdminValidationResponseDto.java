package com.telas.dtos.response;

import com.telas.entities.Ad;
import com.telas.enums.AdValidationType;
import com.telas.enums.Role;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Getter
public final class PendingAdAdminValidationResponseDto implements Serializable {
  @Serial
  private static final long serialVersionUID = 5288515525105234502L;

  private final UUID id;

  private final UUID clientId;

  private final String name;

  private final String clientName;

  private final String clientIdentificationNumber;

  private final Role clientRole;

  private final LocalDate submissionDate;

  private final AdValidationType validation;

  private final long waitingDays;

  private final String link;

  public PendingAdAdminValidationResponseDto(Ad ad, String linkResponse) {
    id = ad.getId();
    clientId = ad.getClient().getId();
    name = ad.getName();
    clientName = ad.getClient().getBusinessName();
    clientIdentificationNumber = ad.getClient().getIdentificationNumber();
    clientRole = ad.getClient().getRole();
    submissionDate = ad.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
    validation = ad.getValidation();
    waitingDays = ChronoUnit.DAYS.between(ad.getCreatedAt(), Instant.now());
    this.link = linkResponse;
  }
}
