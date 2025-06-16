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
  @Column(name = "started_at", columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
  private Instant startedAt;

  @NotAudited
  @Column(name = "ends_at", columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
  private Instant endsAt;

  @ManyToOne
  @JoinColumn(name = "client_id", referencedColumnName = "id")
  private Client client;

  @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL)
  private Set<Payment> payments = new HashSet<>();

  @ManyToMany
  @JoinTable(
          name = "subscriptions_monitors",
          joinColumns = @JoinColumn(name = "subscription_id"),
          inverseJoinColumns = @JoinColumn(name = "monitor_id")
  )
  private Set<Monitor> monitors = new HashSet<>();

  public Subscription(Client client, Cart cart) {
    this.client = client;
    setUsernameCreate(client.getBusinessName());
    bonus = isBonus();
    recurrence = cart.getRecurrence();

    monitors.addAll(cart.getItems().stream()
            .map(CartItem::getMonitor)
            .collect(Collectors.toSet()));
  }

  public void initialize() {
//    if (!upgrade) {
//      startedAt = startedAt == null ? Instant.now() : startedAt;
//      endsAt = isBonus() ? null : recurrence.calculateEndsAt(startedAt);
//    } else {
//      endsAt = recurrence.calculateEndsAt(endsAt);
//    }

    startedAt = startedAt == null ? Instant.now() : startedAt;
    endsAt = isBonus() ? null : recurrence.calculateEndsAt(startedAt);
  }

  public boolean isBonus() {
    if (!Role.PARTNER.equals(client.getRole())) {
      return false;
    }

    Set<UUID> clientAddressesIds = client.getAddresses().stream()
            .map(Address::getId)
            .collect(Collectors.toSet());

    Set<UUID> monitorAddressesIds = monitors.stream()
            .map(monitor -> monitor.getAddress().getId())
            .collect(Collectors.toSet());

    return clientAddressesIds.equals(monitorAddressesIds);
  }

  public BigDecimal getPaidAmount() {
    return payments.stream()
            .filter(payment -> PaymentStatus.COMPLETED.equals(payment.getStatus()))
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public boolean canBeUpgraded(Recurrence recurrence) {
    if (isBonus()
        || Recurrence.MONTHLY.equals(this.recurrence)
        || !SubscriptionStatus.ACTIVE.equals(status)
        || endsAt == null
        || !endsAt.isAfter(Instant.now())) {
      return false;
    }

    if (Recurrence.MONTHLY.equals(recurrence)) {
      long remainingTime = endsAt.getEpochSecond() - Instant.now().getEpochSecond();
      return remainingTime <= SharedConstants.MAX_BILLING_CYCLE_ANCHOR;
    }
    return true;
  }

  public String getMonitorAddresses() {
    return monitors.stream()
            .map(monitor -> monitor.getAddress().getCoordinatesParams())
            .collect(Collectors.joining("\n"));
  }
}
