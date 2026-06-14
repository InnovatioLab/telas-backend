package com.telas.dtos.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BoxOverviewDto(
        UUID boxId,
        String ip,
        String mac,
        String dns,
        boolean active,
        Instant lastSeenAt,
        String reportedVersion,
        boolean reachable,
        String probeDetail,
        int totalMonitors,
        int totalActiveAds,
        List<MonitorSummaryDto> monitors
) {}
