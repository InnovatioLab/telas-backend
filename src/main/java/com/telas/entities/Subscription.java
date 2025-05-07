package com.telas.entities;

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

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "discount", precision = 5, scale = 2, nullable = false)
    private BigDecimal discount = BigDecimal.valueOf(0.00);

    @Column(name = "status", columnDefinition = "subscription_status")
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status = SubscriptionStatus.PENDING;

    @Column(name = "started_at", columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
    private Instant startedAt;

    @Column(name = "ends_at", columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
    private Instant endsAt;

    @OneToOne
    @JoinColumn(name = "plan_id", referencedColumnName = "id")
    private Plan plan;

    @OneToOne(cascade = CascadeType.ALL)
    private Payment payment;

    @OneToOne
    @JoinColumn(name = "client_id", referencedColumnName = "id", nullable = false)
    private Client client;


}
