package com.telas.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public final class MonitorsBoxMinResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -2929124221854520175L;

    private final List<UUID> monitorIds;
    private final String fullAddress;
    private final boolean hasBox;
}