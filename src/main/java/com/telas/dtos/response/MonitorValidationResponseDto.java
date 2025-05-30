package com.telas.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class MonitorValidationResponseDto implements Serializable {
  @Serial
  private static final long serialVersionUID = -9078018412127363619L;

  private UUID monitorId;
  private boolean isValidMonitor;
  private boolean hasLinkedAd;
}
