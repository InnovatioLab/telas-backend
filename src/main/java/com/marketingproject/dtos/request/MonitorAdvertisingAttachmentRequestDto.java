package com.marketingproject.dtos.request;


import com.marketingproject.enums.DisplayType;
import com.marketingproject.shared.constants.valitation.MonitorValidationMessages;
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
public class MonitorAdvertisingAttachmentRequestDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 2896062138730857737L;

    @NotNull(message = MonitorValidationMessages.ADVERTISING_ATTACHMENT_ID_REQUIRED)
    private UUID id;

    private DisplayType displayType = DisplayType.INTERLEAVED;

    @NotNull(message = MonitorValidationMessages.BLOCK_TIME_REQUIRED)
    @Positive(message = MonitorValidationMessages.BLOCK_TIME_INVALID)
    private Integer blockTime;

    @NotNull(message = MonitorValidationMessages.ORDER_REQUIRED)
    @Positive(message = MonitorValidationMessages.ORDER_INVALID)
    private Integer orderIndex;
}