package com.telas.dtos.response;

import com.telas.entities.Monitor;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
public final class MonitorMinResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -2929124221854520175L;

    private final UUID id;
    private final boolean active;
    private final Integer maxBlocks;

    public MonitorMinResponseDto(Monitor entity) {
        id = entity.getId();
        active = entity.isActive();
        maxBlocks = entity.getMaxBlocks();
    }
}