package com.telas.monitoring.entities;

import com.telas.entities.Box;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(schema = "monitoring", name = "box_subnet_routes")
@NoArgsConstructor
public class BoxSubnetRouteEntity {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "box_id", nullable = false)
    private Box box;

    @Column(name = "cidr", nullable = false, length = 64)
    private String cidr;

    @Column(name = "source", nullable = false, length = 32)
    private String source;

    @Column(name = "advertised", nullable = false)
    private boolean advertised;

    @Column(name = "enabled_route", nullable = false)
    private boolean enabledRoute;

    @Column(name = "last_synced_at", nullable = false)
    private Instant lastSyncedAt = Instant.now();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
