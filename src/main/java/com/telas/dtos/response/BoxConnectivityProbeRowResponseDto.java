package com.telas.dtos.response;

import lombok.Builder;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class BoxConnectivityProbeRowResponseDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    UUID boxId;
    String boxIp;
    UUID monitorId;
    String monitorAddressSummary;
    Instant lastProbeAt;
    Boolean reachable;
    String probeDetail;
}
