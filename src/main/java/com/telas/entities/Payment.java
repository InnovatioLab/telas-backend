package com.telas.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.telas.enums.PaymentStatus;
import com.telas.shared.audit.BaseAudit;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.AuditTable;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "payments")
@AuditTable("payments_aud")
@NoArgsConstructor
public class Payment extends BaseAudit implements Serializable {
  @Serial
  private static final long serialVersionUID = 1084934057135367842L;

  @Id
  @GeneratedValue
  @Column(name = "id")
  private UUID id;

  @Column(name = "amount", precision = 10, scale = 2, nullable = false)
  private BigDecimal amount;

  @Column(name = "payment_method", nullable = false)
  private String paymentMethod = "card";

  @Column(name = "currency")
  private String currency = "usd";

  @Column(name = "stripe_payment_id")
  private String stripePaymentId;

  @Column(name = "status", columnDefinition = "payment_status", nullable = false)
  @Enumerated(EnumType.STRING)
  private PaymentStatus status = PaymentStatus.PENDING;

  @JsonIgnore
  @ManyToOne
  @JoinColumn(name = "subscription_id", referencedColumnName = "id")
  private Subscription subscription;

  public Payment(Subscription subscription) {
    this.subscription = subscription;
    amount = subscription.getAmount();
  }
}
