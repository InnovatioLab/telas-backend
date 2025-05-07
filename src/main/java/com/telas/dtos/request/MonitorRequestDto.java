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

    private @Valid List<MonitorAdvertisingAttachmentRequestDto> advertisingAttachments;

    @NotNull(message = MonitorValidationMessages.PARTNER_ID_REQUIRED)
    private UUID partnerId;

    public void validadeAdvertisingAttachmentsOrderIndex() {
        if (!ValidateDataUtils.isNullOrEmpty(advertisingAttachments)) {
            boolean hasDuplicates = advertisingAttachments.stream()
                                            .map(MonitorAdvertisingAttachmentRequestDto::getOrderIndex)
                                            .distinct()
                                            .count() < advertisingAttachments.size();

            if (hasDuplicates) {
                throw new BusinessRuleException(MonitorValidationMessages.ADVERTISING_ATTACHMENT_ORDER_INDEX_DUPLICATED);
            }

            adjustAdvertisingAttachmentsOrder();
        }
    }

    private void adjustAdvertisingAttachmentsOrder() {
        advertisingAttachments.sort(Comparator.comparing(MonitorAdvertisingAttachmentRequestDto::getOrderIndex));

        AtomicInteger sequence = new AtomicInteger(1);
        advertisingAttachments.forEach(attachment -> attachment.setOrderIndex(sequence.getAndIncrement()));
    }
}