package com.telas.dtos.response;

import com.telas.monitoring.entities.IncidentEntity;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
public final class IncidentResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String incidentType;
    private final String severity;
    private final UUID boxId;
    private final UUID monitorId;
    private final Instant openedAt;
    private final Instant closedAt;
    private final Map<String, Object> detailsJson;

    public IncidentResponseDto(IncidentEntity entity) {
        id = entity.getId();
        incidentType = entity.getIncidentType();
        severity = entity.getSeverity();
        boxId = entity.getBox() != null ? entity.getBox().getId() : null;
        monitorId = entity.getMonitor() != null ? entity.getMonitor().getId() : null;
        openedAt = entity.getOpenedAt();
        closedAt = entity.getClosedAt();
        detailsJson = entity.getDetailsJson();
    }
}
