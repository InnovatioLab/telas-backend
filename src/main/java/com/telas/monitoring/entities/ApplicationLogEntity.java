package com.telas.monitoring.entities;

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
@Table(schema = "monitoring", name = "application_logs")
@NoArgsConstructor
public class ApplicationLogEntity {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "level", nullable = false, length = 20)
    private String level;

    @Column(name = "message", nullable = false, columnDefinition = "text")
    private String message;

    @Column(name = "source", nullable = false, length = 50)
    private String source;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "stack_trace", columnDefinition = "text")
    private String stackTrace;

    @Column(name = "endpoint")
    private String endpoint;

    @Column(name = "client_id")
    private UUID clientId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json")
    private Map<String, Object> metadataJson;
}
