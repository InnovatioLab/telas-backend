package com.telas.dtos.response;

import com.telas.entities.Monitor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public final class SubscriptionMonitorResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -7144327643566339527L;

    private UUID id;
    private String type;
    private String addressData;
    private List<MonitorAdResponseDto> adLinks;

    public SubscriptionMonitorResponseDto(Monitor monitor) {
        id = monitor.getId();
        type = monitor.getType().name();
        addressData = monitor.getAddress().getCoordinatesParams();
    }

    public SubscriptionMonitorResponseDto(Monitor monitor, List<MonitorAdResponseDto> adLinks) {
        id = monitor.getId();
        type = monitor.getType().name();
        addressData = monitor.getAddress().getCoordinatesParams();
        this.adLinks = adLinks;
    }
}
