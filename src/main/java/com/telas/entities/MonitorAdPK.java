package com.telas.entities;

import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class MonitorAdvertisingAttachmentPK implements Serializable {
    @Serial
    private static final long serialVersionUID = -8798763358641899098L;

    @ManyToOne
    @JoinColumn(name = "monitor_id", nullable = false)
    private Monitor monitor;

    @ManyToOne
    @JoinColumn(name = "advertising_attachment_id", nullable = false)
    private AdvertisingAttachment advertisingAttachment;

    @Override
    public int hashCode() {
        int result = (monitor != null ? monitor.hashCode() : 0);
        result = 31 * result + (advertisingAttachment != null ? advertisingAttachment.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MonitorAdvertisingAttachmentPK that = (MonitorAdvertisingAttachmentPK) o;
        return Objects.equals(monitor, that.monitor) && Objects.equals(advertisingAttachment, that.advertisingAttachment);
    }
}