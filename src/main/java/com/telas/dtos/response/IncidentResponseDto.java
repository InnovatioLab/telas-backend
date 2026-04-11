package com.telas.dtos.response;

import com.telas.entities.Contact;
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
    private final Instant acknowledgedAt;
    private final String acknowledgeReason;
    private final UUID acknowledgedById;
    private final String acknowledgedByEmail;
    private final Map<String, Object> detailsJson;

    public IncidentResponseDto(IncidentEntity entity) {
        id = entity.getId();
        incidentType = entity.getIncidentType();
        severity = entity.getSeverity();
        boxId = entity.getBox() != null ? entity.getBox().getId() : null;
        monitorId = entity.getMonitor() != null ? entity.getMonitor().getId() : null;
        openedAt = entity.getOpenedAt();
        closedAt = entity.getClosedAt();
        acknowledgedAt = entity.getAcknowledgedAt();
        acknowledgeReason = entity.getAcknowledgeReason();
        if (entity.getAcknowledgedBy() != null) {
            acknowledgedById = entity.getAcknowledgedBy().getId();
            Contact contact = entity.getAcknowledgedBy().getContact();
            acknowledgedByEmail = contact != null ? contact.getEmail() : null;
        } else {
            acknowledgedById = null;
            acknowledgedByEmail = null;
        }
        detailsJson = entity.getDetailsJson();
    }
}
