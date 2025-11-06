package com.telas.dtos.request;


import com.telas.shared.constants.valitation.BoxValidationMessages;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RemoveBoxMonitorsAdRequestDto implements Serializable {
  @Serial
  private static final long serialVersionUID = -3963846843873646628L;

  @NotNull(message = BoxValidationMessages.MONITOR_ID_NOT_NULL)
  private UUID monitorId;

  @NotNull(message = BoxValidationMessages.FILE_NAMES_NOT_NULL)
  @NotEmpty(message = BoxValidationMessages.FILE_NAMES_NOT_EMPTY)
  private List<String> fileNames;
}