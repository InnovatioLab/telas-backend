package com.telas.dtos.response;

import com.telas.enums.BoxScriptVersionStatus;
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
    String reportedBoxScriptVersion;
    String targetBoxScriptVersion;
    BoxScriptVersionStatus boxScriptVersionStatus;
    String reportedGitSha;
    String reportedBuildId;
}
