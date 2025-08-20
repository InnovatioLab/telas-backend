package com.telas.dtos.response;

import com.telas.entities.Monitor;
import lombok.Getter;
import org.springframework.util.ObjectUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
public final class SubscriptionMonitorMinResponseDto implements Serializable {
  @Serial
  private static final long serialVersionUID = -7144327643566339527L;

  private final UUID id;
  private final String type;
  private final double size;
  private final String addressData;

  public SubscriptionMonitorMinResponseDto(Monitor monitor) {
    id = monitor.getId();
    type = monitor.getType().name();
    size = monitor.getSize().doubleValue();
    addressData = ObjectUtils.isEmpty(monitor.getAddress().getLocationName()) ? monitor.getAddress().getCoordinatesParams() : monitor.getAddress().getLocationName();
  }
}
