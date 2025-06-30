package com.telas.dtos.response;

import com.telas.entities.Address;
import com.telas.entities.Monitor;
import com.telas.enums.MonitorType;
import com.telas.shared.constants.SharedConstants;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Getter
public final class MonitorResponseDto implements Serializable {
  @Serial
  private static final long serialVersionUID = -2929124221854520175L;

  private final UUID id;
  private final boolean active;
  private final MonitorType type;
  private final BigDecimal size;
  private final Integer adsDailyDisplayTimeInMinutes;
  private final Double latitude;
  private final Double longitude;
  private final Address address;
  private final List<MonitorAdResponseDto> adLinks;

  public MonitorResponseDto(Monitor entity, List<MonitorAdResponseDto> adLinks) {
    id = entity.getId();
    active = entity.isActive();
    type = entity.getType();
    size = entity.getSize();
    latitude = entity.getAddress().getLatitude();
    longitude = entity.getAddress().getLongitude();
    address = entity.getAddress();
    this.adLinks = adLinks;
    adsDailyDisplayTimeInMinutes = calculateAdsDailyDisplayTimeInMinutes(entity.getMonitorAds().size(), Instant.now());
  }

  private Integer calculateAdsDailyDisplayTimeInMinutes(int adsCount, Instant now) {
    if (adsCount == 0) {
      return 0;
    }

    LocalTime localTime = LocalTime.ofInstant(now, java.time.ZoneId.of(SharedConstants.ZONE_ID));
    int secondsOfDay = localTime.toSecondOfDay();
    int loopDuration = adsCount * SharedConstants.AD_DISPLAY_TIME_IN_SECONDS;
    int loops = secondsOfDay / loopDuration;
    int totalSeconds = loops * SharedConstants.AD_DISPLAY_TIME_IN_SECONDS;
    return totalSeconds / SharedConstants.TOTAL_SECONDS_IN_A_MINUTE;
  }
}