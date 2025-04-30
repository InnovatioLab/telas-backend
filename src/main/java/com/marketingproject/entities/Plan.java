package com.marketingproject.entities;

import com.marketingproject.enums.DefaultStatus;
import com.marketingproject.shared.audit.BaseAudit;
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
@Table(name = "plans")
@AuditTable("plans_aud")
@NoArgsConstructor
public class Plan extends BaseAudit implements Serializable {
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

    @Column(name = "monthly_price", precision = 10, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(name = "quarterly_price", precision = 10, scale = 2)
    private BigDecimal quarterlyPrice;

    @Column(name = "semi_annual_price", precision = 10, scale = 2)
    private BigDecimal semiAnnualPrice;

    @Column(name = "yearly_price", precision = 10, scale = 2)
    private BigDecimal yearlyPrice;

    @Column(name = "status", columnDefinition = "default_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private DefaultStatus status;

    @Column(name = "monitors_quantity", nullable = false)
    private Integer monitorsQuantity;

    @Column(name = "advertising_attachments", nullable = false)
    private Integer advertisingAttachmentsQuantity;

    @Column(name = "inactivated_at", columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
    private Instant inactivatedAt;


}
