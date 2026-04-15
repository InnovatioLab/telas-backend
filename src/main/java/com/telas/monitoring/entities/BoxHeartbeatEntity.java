package com.telas.monitoring.entities;

import com.telas.entities.Box;
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
@Table(schema = "monitoring", name = "box_heartbeat")
@NoArgsConstructor
public class BoxHeartbeatEntity {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "box_id", nullable = false, unique = true)
    private Box box;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "reported_version", length = 64)
    private String reportedVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json")
    private Map<String, Object> metadataJson;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
