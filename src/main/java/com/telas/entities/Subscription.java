package com.telas.entities;

import com.telas.enums.Recurrence;
import com.telas.enums.Role;
import com.telas.enums.SubscriptionStatus;
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
public class Subscription implements Serializable {
  @Serial
  private static final long serialVersionUID = 1084934057135367842L;

  @Id
  @GeneratedValue
  @Column(name = "id")
  private UUID id;

  @Column(name = "amount", precision = 10, scale = 2, nullable = false)
  private BigDecimal amount = BigDecimal.valueOf(0.00);

//  @Column(name = "discount", precision = 5, scale = 2, nullable = false)
//  private BigDecimal discount = BigDecimal.valueOf(0.00);

  @Column(name = "recurrence", nullable = false)
  @Enumerated(EnumType.STRING)
  private Recurrence recurrence;

  @Column(name = "fl_bonus")
  private boolean bonus = false;

  @Column(name = "status", columnDefinition = "subscription_status")
  @Enumerated(EnumType.STRING)
  private SubscriptionStatus status = SubscriptionStatus.PENDING;

  @NotAudited
  @Column(name = "started_at", columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
  private Instant startedAt;

  @NotAudited
  @Column(name = "ends_at", columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
  private Instant endsAt;

  @OneToOne
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
    bonus = isBonus();
    recurrence = cart.getRecurrence();

    monitors.addAll(cart.getItems().stream()
            .map(CartItem::getMonitor)
            .toList());
  }

  public void initialize() {
    startedAt = Instant.now();
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
}
