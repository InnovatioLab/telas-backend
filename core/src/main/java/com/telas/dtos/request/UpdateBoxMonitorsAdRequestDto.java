package com.telas.dtos.request;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.telas.shared.constants.valitation.BoxValidationMessages;
import com.telas.shared.utils.TrimStringDeserializer;
import jakarta.validation.constraints.NotNull;
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
public class UpdateBoxMonitorsAdRequestDto implements Serializable {
  @Serial
  private static final long serialVersionUID = -3963846843873646628L;

  @NotNull(message = BoxValidationMessages.MONITOR_ID_NOT_NULL)
  private UUID monitorId;

  @JsonDeserialize(using = TrimStringDeserializer.class)
  private String fileName;

  @JsonDeserialize(using = TrimStringDeserializer.class)
  private String link;
}