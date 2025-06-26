package com.telas.dtos.request;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.BoxValidationMessages;
import com.telas.shared.utils.TrimStringDeserializer;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
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

  @NotEmpty(message = BoxValidationMessages.IP_NOT_EMPTY)
  @Pattern(regexp = SharedConstants.REGEX_IP, message = BoxValidationMessages.IP_INVALID)
  @JsonDeserialize(using = TrimStringDeserializer.class)
  private String ip;

  @NotEmpty(message = BoxValidationMessages.MONITOR_IDS_NOT_EMPTY)
  private List<UUID> monitorIds = new ArrayList<>();
}