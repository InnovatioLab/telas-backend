package com.marketingproject.dtos.response;

import com.marketingproject.entities.MonitorAdvertisingAttachment;
import com.marketingproject.enums.DisplayType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public final class MonitorAdvertisingAttachmentResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -7144327643566339527L;


    private UUID attachmentId;

    private String attachmentLink;

    private DisplayType displayType;

    private Integer blockTime;

    private Integer orderIndex;

    public MonitorAdvertisingAttachmentResponseDto(MonitorAdvertisingAttachment entity, String attachmentLink) {
        attachmentId = entity.getAdvertisingAttachment().getId();
        displayType = entity.getDisplayType();
        blockTime = entity.getBlockTime();
        orderIndex = entity.getOrderIndex();
        this.attachmentLink = attachmentLink;
    }
}
