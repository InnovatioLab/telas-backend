package com.telas.dtos.request;


import com.telas.shared.constants.valitation.MonitorValidationMessages;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MonitorAdRequestDto implements Serializable {
  @Serial
  private static final long serialVersionUID = 2896062138730857737L;

  @NotNull(message = MonitorValidationMessages.AD_ID_REQUIRED)
  private UUID id;

  @NotNull(message = MonitorValidationMessages.ORDER_REQUIRED)
  @Positive(message = MonitorValidationMessages.ORDER_INVALID)
  private Integer orderIndex;
}