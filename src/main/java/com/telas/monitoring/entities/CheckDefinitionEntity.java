package com.telas.monitoring.entities;

import com.telas.entities.Box;
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
@Table(schema = "monitoring", name = "check_definitions")
@NoArgsConstructor
public class CheckDefinitionEntity {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @Column(name = "check_type", nullable = false, length = 50)
    private String checkType;

    @Column(name = "interval_seconds", nullable = false)
    private Integer intervalSeconds;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "box_id")
    private Box box;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitor_id")
    private Monitor monitor;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params_json")
    private Map<String, Object> paramsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
