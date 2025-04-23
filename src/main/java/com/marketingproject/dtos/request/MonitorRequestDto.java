package com.marketingproject.dtos.request;


import com.marketingproject.enums.MonitorType;
import com.marketingproject.shared.constants.valitation.MonitorValidationMessages;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MonitorRequestDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -3963846843873646628L;

    //    @NotNull(message = MonitorValidationMessages.SIZE_REQUIRED)
    @Digits(integer = 3, fraction = 2, message = MonitorValidationMessages.SIZE_INVALID)
    private BigDecimal size;

    private MonitorType type = MonitorType.BASIC;

    private Boolean active;

    private Double latitude;

    private Double longitude;

    @NotNull(message = MonitorValidationMessages.ADDRESS_REQUIRED)
    private @Valid AddressRequestDto address;

    private List<UUID> advertisingAttachmentsIds;
}