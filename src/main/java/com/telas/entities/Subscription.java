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
  private BigDecimal amount;

  @Column(name = "discount", precision = 5, scale = 2, nullable = false)
  private BigDecimal discount = BigDecimal.valueOf(0.00);

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

  @OneToOne(mappedBy = "subscription")
  private Payment payment;

  @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL)
  private Set<SubscriptionMonitor> subscriptionMonitors = new HashSet<>();

  public Subscription(Client client, Cart cart) {
    this.client = client;
    bonus = Role.PARTNER.equals(client.getRole());
    recurrence = cart.getRecurrence();

    subscriptionMonitors.addAll(
            cart.getItems().stream()
                    .map(cartItem -> new SubscriptionMonitor(this, cartItem))
                    .toList()
    );
  }

  public void initialize() {
    startedAt = Instant.now();
    endsAt = isBonus() ? null : recurrence.calculateEndsAt(startedAt);
  }
}
