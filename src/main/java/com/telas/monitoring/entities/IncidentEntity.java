package com.telas.monitoring.entities;

import com.telas.entities.Box;
import com.telas.entities.Client;
import com.telas.entities.Monitor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(schema = "monitoring", name = "incidents")
@NoArgsConstructor
public class IncidentEntity {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @Column(name = "incident_type", nullable = false, length = 50)
    private String incidentType;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "box_id")
    private Box box;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitor_id")
    private Monitor monitor;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt = Instant.now();

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "acknowledge_reason")
    private String acknowledgeReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acknowledged_by")
    private Client acknowledgedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details_json")
    private Map<String, Object> detailsJson;
}
