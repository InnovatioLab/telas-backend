package com.telas.dtos.response;

import lombok.Builder;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Value
@Builder
public class BoxScriptPendingCommandResponseDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    UUID commandId;
    String targetVersion;
    String artifactUrl;
    String sha256;
}
