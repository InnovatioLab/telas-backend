package com.telas.dtos.response;

import com.telas.monitoring.entities.ApplicationLogEntity;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
public final class ApplicationLogResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final Instant createdAt;
    private final String level;
    private final String message;
    private final String source;
    private final String correlationId;
    private final String stackTrace;
    private final String endpoint;
    private final UUID clientId;
    private final Map<String, Object> metadata;

    public ApplicationLogResponseDto(ApplicationLogEntity entity) {
        id = entity.getId();
        createdAt = entity.getCreatedAt();
        level = entity.getLevel();
        message = entity.getMessage();
        source = entity.getSource();
        correlationId = entity.getCorrelationId();
        stackTrace = entity.getStackTrace();
        endpoint = entity.getEndpoint();
        clientId = entity.getClientId();
        metadata = entity.getMetadataJson();
    }
}
