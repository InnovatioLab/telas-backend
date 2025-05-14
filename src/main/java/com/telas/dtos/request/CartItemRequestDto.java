package com.telas.dtos.request;


import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.CartValidationMessages;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class CartItemRequestDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -3963846843873646628L;

    @NotNull(message = CartValidationMessages.MONITOR_ID_REQUIRED)
    private UUID monitorId;

    @Min(value = SharedConstants.MIN_QUANTITY_MONITOR_BLOCK, message = CartValidationMessages.MIN_QUANTITY_MONITOR_BLOCK)
    @Max(value = SharedConstants.MAX_QUANTITY_MONITOR_BLOCK, message = CartValidationMessages.MAX_QUANTITY_MONITOR_BLOCK)
    private Integer blockQuantity = 1;
}