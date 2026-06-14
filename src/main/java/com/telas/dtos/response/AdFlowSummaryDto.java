package com.telas.dtos.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdFlowSummaryDto(
        UUID adRequestId,
        String clientName,
        String adTitle,
        String adMimeType,
        String adPreviewUrl,
        String currentStatus,
        Instant lastEventAt,
        List<FlowEventDto> events
) {}
