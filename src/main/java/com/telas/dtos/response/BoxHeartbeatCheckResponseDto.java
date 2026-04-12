package com.telas.dtos.response;

import lombok.Builder;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class BoxHeartbeatCheckResponseDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    UUID boxId;
    Instant lastHeartbeatAt;
    Long secondsSinceHeartbeat;
    boolean heartbeatOnline;
    String heartbeatStatus;
    long staleAfterSeconds;
}
