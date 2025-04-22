package com.marketingproject.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.marketingproject.enums.MonitorType;
import com.marketingproject.shared.audit.BaseAudit;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "monitors")
@NoArgsConstructor
public class Monitor extends BaseAudit implements Serializable {
    @Serial
    private static final long serialVersionUID = 1084934057135367842L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @Column(name = "fl_active", nullable = false)
    private boolean active = true;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private MonitorType type = MonitorType.BASIC;

    @Column(name = "size_in_inches", precision = 5, scale = 2, nullable = false)
    private BigDecimal size = BigDecimal.valueOf(0.00);

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    private Address address;

    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name = "monitor_advertising_attachments",
            joinColumns = @JoinColumn(name = "monitor_id"),
            inverseJoinColumns = @JoinColumn(name = "advertising_attachment_id")
    )
    private Set<AdvertisingAttachment> advertisingAttachments = new HashSet<AdvertisingAttachment>();


}
