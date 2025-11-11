package com.telas.entities;

import com.telas.enums.PaymentStatus;
import com.telas.enums.Recurrence;
import com.telas.enums.Role;
import com.telas.enums.SubscriptionStatus;
import com.telas.shared.audit.BaseAudit;
import com.telas.shared.constants.SharedConstants;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.NotAudited;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
@Table(name = "subscriptions")
@AuditTable("subscriptions_aud")
@NoArgsConstructor
public class Subscription extends BaseAudit implements Serializable {
    @Serial
    private static final long serialVersionUID = 1084934057135367842L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @Column(name = "recurrence", nullable = false)
    @Enumerated(EnumType.STRING)
    private Recurrence recurrence;

    @Column(name = "stripe_id")
    private String stripeId;

    @Column(name = "fl_bonus")
    private boolean bonus = false;

    @Column(name = "status", columnDefinition = "subscription_status")
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status = SubscriptionStatus.PENDING;

    @Column(name = "fl_upgrade", nullable = false)
    private boolean upgrade = false;

    @NotAudited
    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "started_at", columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
    private Instant startedAt;

    @Column(name = "ends_at", columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
    private Instant endsAt;

    @ManyToOne
    @JoinColumn(name = "client_id", referencedColumnName = "id")
    private Client client;

    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL)
    private Set<Payment> payments = new HashSet<>();

    @OneToMany(mappedBy = "id.subscription", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SubscriptionMonitor> subscriptionMonitors = new HashSet<>();

    public Subscription(Client client, Cart cart) {
        this.client = client;
        setUsernameCreate(client.getBusinessName());
        
        cart.getItems().forEach(item -> {
            Monitor monitor = item.getMonitor();
            SubscriptionMonitor subscriptionMonitor = new SubscriptionMonitor(
                    this,
                    monitor,
                    item.getBlockQuantity()
            );
            subscriptionMonitors.add(subscriptionMonitor);
        });

        bonus = isPartnerBonus();
        recurrence = bonus ? Recurrence.MONTHLY : cart.getRecurrence();
        status = bonus ? SubscriptionStatus.ACTIVE : SubscriptionStatus.PENDING;

        if (bonus) {
            startedAt = Instant.now();
        }
    }

    public void initialize() {
        startedAt = startedAt == null ? Instant.now() : startedAt;
        endsAt = bonus ? null : recurrence.calculateEndsAt(startedAt);
    }

    private boolean isPartnerBonus() {
        if (!Role.PARTNER.equals(client.getRole())) {
            return false;
        }

        Set<UUID> clientAddressesIds = client.getAddresses().stream()
                .map(Address::getId)
                .collect(Collectors.toSet());

        Set<UUID> monitorAddressesIds = getMonitors().stream()
                .map(monitor -> monitor.getAddress().getId())
                .collect(Collectors.toSet());

        return clientAddressesIds.containsAll(monitorAddressesIds);
    }

    public boolean ableToUpgrade() {
        return !bonus
                && !isUpgrade()
                && !Recurrence.MONTHLY.equals(recurrence)
                && SubscriptionStatus.ACTIVE.equals(status)
                && endsAt != null
                && endsAt.isAfter(Instant.now())
                && getMonitors().stream().allMatch(monitor -> monitor.getBox() != null && monitor.getBox().isActive());
    }

    public boolean ableToRenew() {
        return !bonus
                && !Recurrence.MONTHLY.equals(recurrence)
                && SubscriptionStatus.ACTIVE.equals(status)
                && endsAt != null
                && endsAt.isAfter(Instant.now())
                && getMonitors().stream().allMatch(monitor -> monitor.getBox() != null && monitor.getBox().isActive());
    }

    public BigDecimal getPaidAmount() {
        return payments.stream()
                .filter(payment -> PaymentStatus.COMPLETED.equals(payment.getStatus()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Set<Monitor> getMonitors() {
        return subscriptionMonitors.stream()
                .map(SubscriptionMonitor::getMonitor)
                .collect(Collectors.toSet());
    }

    public String getMonitorAddresses() {
        return getMonitors().stream()
                .map(monitor -> monitor.getAddress().getCoordinatesParams())
                .collect(Collectors.joining("\n"));
    }
}
