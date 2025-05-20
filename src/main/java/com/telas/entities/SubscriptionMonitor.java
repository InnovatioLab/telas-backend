package com.telas.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.telas.shared.audit.BaseAudit;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.AuditTable;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "subscription_monitors")
@AuditTable("subscription_monitors_aud")
public class SubscriptionMonitor extends BaseAudit implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false)
  private UUID id;

  @JsonIgnore
  @ManyToOne
  @JoinColumn(name = "subscription_id", nullable = false)
  private Subscription subscription;

  @ManyToOne
  @JoinColumn(name = "monitor_id", nullable = false)
  private Monitor monitor;

  @Column(name = "block_quantity", nullable = false)
  private Integer blockQuantity;

  public SubscriptionMonitor(Subscription subscription, CartItem cartItem) {
    this.subscription = subscription;
    monitor = cartItem.getMonitor();
    blockQuantity = cartItem.getBlockQuantity();
  }
}