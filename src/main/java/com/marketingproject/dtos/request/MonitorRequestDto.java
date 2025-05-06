package com.marketingproject.dtos.request;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.marketingproject.enums.MonitorType;
import com.marketingproject.shared.constants.SharedConstants;
import com.marketingproject.shared.constants.valitation.MonitorValidationMessages;
import com.marketingproject.shared.utils.TrimStringDeserializer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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

    @Digits(integer = 3, fraction = 2, message = MonitorValidationMessages.SIZE_INVALID)
    private BigDecimal size;

    @NotNull(message = MonitorValidationMessages.MAX_BLOCKS_REQUIRED)
    @Positive(message = MonitorValidationMessages.MAX_BLOCKS_INVALID)
    private Integer maxBlocks;

    @Size(max = SharedConstants.TAMANHO_NOME_ANEXO, message = MonitorValidationMessages.LOCATION_DESCRIPTION_SIZE)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String locationDescription;

    private MonitorType type = MonitorType.BASIC;

    private Boolean active;

    private Double latitude;

    private Double longitude;

    @NotNull(message = MonitorValidationMessages.ADDRESS_REQUIRED)
    private @Valid AddressRequestDto address;

    private List<UUID> advertisingAttachmentsIds;

    @NotNull(message = MonitorValidationMessages.PARTNER_ID_REQUIRED)
    private UUID partnerId;
}