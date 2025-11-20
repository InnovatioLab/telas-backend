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
public class SubscriptionMonitorPK implements Serializable {
    @Serial
    private static final long serialVersionUID = -8798763358641899098L;

    @ManyToOne
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @ManyToOne
    @JoinColumn(name = "monitor_id", nullable = false)
    private Monitor monitor;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        SubscriptionMonitorPK that = (SubscriptionMonitorPK) o;
        return getSubscription().equals(that.getSubscription()) && getMonitor().equals(that.getMonitor());
    }

    @Override
    public int hashCode() {
        int result = getSubscription().hashCode();
        result = 31 * result + getMonitor().hashCode();
        return result;
    }
}

