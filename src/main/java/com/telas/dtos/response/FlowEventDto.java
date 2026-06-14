package com.telas.dtos.response;

import java.time.Instant;

public record FlowEventDto(
        Instant occurredAt,
        String eventType,
        String actorType,
        String actorName,
        String label,
        String detail
) {}
