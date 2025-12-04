package com.telas.dtos.response;

import com.telas.entities.Monitor;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
public final class SubscriptionMonitorMinResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -7144327643566339527L;

    private final UUID id;
    private final String addressData;

    public SubscriptionMonitorMinResponseDto(Monitor monitor) {
        id = monitor.getId();
        addressData = monitor.getAddress().getCoordinatesParamsFormated();
    }
}
