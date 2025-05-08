package com.telas.dtos.request;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.telas.enums.MonitorType;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.shared.utils.TrimStringDeserializer;
import com.telas.shared.utils.ValidateDataUtils;
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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MonitorRequestDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -3963846843873646628L;

    @Digits(integer = 3, fraction = 2, message = MonitorValidationMessages.SIZE_INVALID)
    private BigDecimal size;

    @NotNull(message = MonitorValidationMessages.ADDRESS_ID_REQUIRED)
    private UUID addressId;

    @Positive(message = MonitorValidationMessages.MAX_BLOCKS_INVALID)
    private Integer maxBlocks;

    @Size(max = SharedConstants.TAMANHO_NOME_ANEXO, message = MonitorValidationMessages.LOCATION_DESCRIPTION_SIZE)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String locationDescription;

    private MonitorType type = MonitorType.BASIC;

    private Boolean active;

    private @Valid List<MonitorAdRequestDto> ads;

    public void validate() {
        validadeAdsOrderIndex();
        validateMaxDisplayTime();
    }

    private void validateMaxDisplayTime() {
        if (ValidateDataUtils.isNullOrEmpty(ads)) {
            return;
        }

        int sumDisplayTimeInSeconds = ads.stream()
                .mapToInt(MonitorAdRequestDto::getBlockTime)
                .sum();

        int maxAllowedTime = (maxBlocks == null)
                ? SharedConstants.MAX_MONITOR_DISPLAY_TIME
                : SharedConstants.MONITOR_ADS_TIME_IN_SECONDS * maxBlocks;

        if (sumDisplayTimeInSeconds > maxAllowedTime) {
            throw new BusinessRuleException(MonitorValidationMessages.SUM_BLOCK_TIME_INVALID);
        }
    }

    private void validadeAdsOrderIndex() {
        if (!ValidateDataUtils.isNullOrEmpty(ads)) {
            boolean hasDuplicates = ads.stream()
                                            .map(MonitorAdRequestDto::getOrderIndex)
                                            .distinct()
                                            .count() < ads.size();

            if (hasDuplicates) {
                throw new BusinessRuleException(MonitorValidationMessages.ADS_ORDER_INDEX_DUPLICATED);
            }

            adjustAdsOrder();
        }
    }

    private void adjustAdsOrder() {
        ads.sort(Comparator.comparing(MonitorAdRequestDto::getOrderIndex));

        AtomicInteger sequence = new AtomicInteger(1);
        ads.forEach(attachment -> attachment.setOrderIndex(sequence.getAndIncrement()));
    }
}