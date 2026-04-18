package com.telas.monitoring.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(schema = "monitoring", name = "box_connectivity_probe")
@NoArgsConstructor
public class BoxConnectivityProbeEntity {

    @Id
    @Column(name = "box_id")
    private UUID boxId;

    @Column(name = "last_probe_at", nullable = false)
    private Instant lastProbeAt;

    @Column(name = "reachable", nullable = false)
    private boolean reachable;

    @Column(name = "probe_detail")
    private String probeDetail;

    @Column(name = "box_ip", length = 128)
    private String boxIp;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
