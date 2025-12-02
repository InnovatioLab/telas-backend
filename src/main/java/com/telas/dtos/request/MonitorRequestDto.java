package com.telas.dtos.request;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.shared.utils.ValidateDataUtils;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MonitorRequestDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -3963846843873646628L;

    private UUID addressId;

    private AddressRequestDto address;

    private Boolean active;

    private @Valid List<MonitorAdRequestDto> ads = new ArrayList<>();

    public void validate() {
        validateAddress();
        validadeAdsOrderIndex();
        validateBlockQuantity();
    }

    private void validateBlockQuantity() {
        if (ValidateDataUtils.isNullOrEmpty(ads)) {
            return;
        }

        int partnerSlots = SharedConstants.PARTNER_RESERVED_SLOTS;
        int maxTotal = SharedConstants.MAX_MONITOR_ADS;

        long partnerCount = ads.stream()
                .map(MonitorAdRequestDto::getBlockQuantity)
                .filter(q -> q != null && q == partnerSlots)
                .count();

        if (partnerCount > SharedConstants.MAX_ADS_PER_CLIENT) {
            throw new BusinessRuleException(MonitorValidationMessages.BLOCK_QUANTITY_PARTNER_DUPLICATED);
        }

        int totalBlockQuantity = ads.stream()
                .map(MonitorAdRequestDto::getBlockQuantity)
                .mapToInt(Integer::intValue)
                .sum();

        if (totalBlockQuantity > maxTotal || ads.size() > maxTotal) {
            throw new BusinessRuleException(MonitorValidationMessages.MONITOR_BLOCKS_BEYOND_LIMIT);
        }
    }

    private void validateAddress() {
        if (address == null && addressId == null) {
            throw new BusinessRuleException(MonitorValidationMessages.ADDRESS_REQUIRED);
        }

        if (address != null && addressId != null) {
            throw new BusinessRuleException(MonitorValidationMessages.ADDRESS_ID_AND_ADDRESS_BOTH_PROVIDED);
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