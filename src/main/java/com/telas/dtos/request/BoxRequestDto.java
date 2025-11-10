package com.telas.dtos.request;


import com.telas.shared.constants.valitation.BoxValidationMessages;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BoxRequestDto implements Serializable {
  @Serial
  private static final long serialVersionUID = 2896062138730857737L;

  @NotNull(message = BoxValidationMessages.BOX_ADDRESS_ID_REQUIRED)
  private UUID boxAddressId;

  @NotNull(message = BoxValidationMessages.MONITOR_ID_REQUIRED)
  private UUID monitorId;

  private boolean active = true;
}