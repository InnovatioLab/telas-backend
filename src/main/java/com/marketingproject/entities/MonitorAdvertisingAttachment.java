package com.marketingproject.entities;

import com.marketingproject.dtos.request.MonitorAdvertisingAttachmentRequestDto;
import com.marketingproject.enums.DisplayType;
import com.marketingproject.shared.audit.BaseAudit;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.AuditTable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "monitors_advertising_attachments")
@AuditTable("monitors_advertising_attachments_aud")
public class MonitorAdvertisingAttachment extends BaseAudit implements Serializable {
    @Serial
    private static final long serialVersionUID = 4636281925583628366L;

    @EmbeddedId
    private MonitorAdvertisingAttachmentPK id = new MonitorAdvertisingAttachmentPK();

    @Column(name = "display_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private DisplayType displayType = DisplayType.INTERLEAVED;

    @Column(name = "block_time", nullable = false)
    private Integer blockTime;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    public MonitorAdvertisingAttachment(MonitorAdvertisingAttachmentRequestDto request, Monitor monitor, AdvertisingAttachment advertisingAttachment) {
        id.setMonitor(monitor);
        id.setAdvertisingAttachment(advertisingAttachment);
        displayType = request.getDisplayType();
        blockTime = request.getBlockTime();
        orderIndex = request.getOrderIndex();
    }

    public Monitor getMonitor() {
        return id.getMonitor();
    }

    public void setMonitor(Monitor monitor) {
        id.setMonitor(monitor);
    }

    public AdvertisingAttachment getAdvertisingAttachment() {
        return id.getAdvertisingAttachment();
    }

    public void setAdvertisingAttachment(AdvertisingAttachment advertisingAttachment) {
        id.setAdvertisingAttachment(advertisingAttachment);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MonitorAdvertisingAttachment monitorAdvertisingAttachment = (MonitorAdvertisingAttachment) o;
        return Objects.equals(id, monitorAdvertisingAttachment.id);
    }
}