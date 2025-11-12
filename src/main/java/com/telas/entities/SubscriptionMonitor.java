package com.telas.entities;

import com.telas.shared.constants.SharedConstants;
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
@Entity
@Table(name = "subscriptions_monitors")
@AuditTable("subscriptions_monitors_aud")
@NoArgsConstructor
public class SubscriptionMonitor implements Serializable {
    @Serial
    private static final long serialVersionUID = 1084934057135367842L;

    @EmbeddedId
    private SubscriptionMonitorPK id = new SubscriptionMonitorPK();

    @Column(name = "slots_quantity", nullable = false)
    private Integer slotsQuantity = 1;

    public SubscriptionMonitor(Subscription subscription, Monitor monitor, Integer slotsQuantity) {
        id.setSubscription(subscription);
        id.setMonitor(monitor);
        this.slotsQuantity = slotsQuantity != null ? slotsQuantity : 1;
    }

    public SubscriptionMonitor(Subscription subscription, Monitor monitor) {
        id.setSubscription(subscription);
        id.setMonitor(monitor);
        this.slotsQuantity = SharedConstants.PARTNER_RESERVED_SLOTS;
    }

    @Transient
    public Subscription getSubscription() {
        return id.getSubscription();
    }

    @Transient
    public Monitor getMonitor() {
        return id.getMonitor();
    }

    public void setSubscription(Subscription subscription) {
        id.setSubscription(subscription);
    }

    public void setMonitor(Monitor monitor) {
        id.setMonitor(monitor);
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

        SubscriptionMonitor subscriptionMonitor = (SubscriptionMonitor) o;
        return Objects.equals(id, subscriptionMonitor.id);
    }
}

