package com.telas.entities;

import com.telas.enums.Recurrence;
import com.telas.enums.SubscriptionStatus;
import com.telas.shared.audit.BaseAudit;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.AuditTable;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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

    @Column(name = "started_at", columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
    private Instant startedAt;

    @Column(name = "ends_at", columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
    private Instant endsAt;

    @OneToOne
    @JoinColumn(name = "client_id", referencedColumnName = "id")
    private Client client;

    @OneToOne(cascade = CascadeType.ALL)
    private Payment payment;

    public Subscription(BigDecimal amount, BigDecimal discount, Recurrence recurrence, boolean bonus, SubscriptionStatus status) {
        this.amount = amount;
        this.discount = discount;
        this.recurrence = recurrence;
        this.bonus = bonus;
        this.status = status;
    }

    public void initialize() {
        this.startedAt = Instant.now();
        this.endsAt = recurrence.calculateEndsAt(this.startedAt);
    }
}
