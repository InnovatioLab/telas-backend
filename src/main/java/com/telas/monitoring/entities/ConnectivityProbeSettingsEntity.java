package com.telas.monitoring.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(schema = "monitoring", name = "connectivity_probe_settings")
@NoArgsConstructor
public class ConnectivityProbeSettingsEntity {

    @Id
    @Column(name = "id")
    private Short id = 1;

    @Column(name = "box_connectivity_probe_interval_ms", nullable = false)
    private long boxConnectivityProbeIntervalMs;
}
