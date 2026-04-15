package com.telas.dtos.response;

import lombok.Builder;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class MonitoringTestingRowDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    UUID boxId;
    String boxIp;
    boolean boxActive;
    Instant lastHeartbeatAt;
    boolean heartbeatOnline;
    String heartbeatStatus;
    UUID monitorId;
    String monitorAddressSummary;
    Boolean monitorActive;
    UUID smartPlugId;
    String smartPlugMac;
    String smartPlugVendor;
    Boolean smartPlugEnabled;
    UUID boxSmartPlugId;
    String boxSmartPlugMac;
    String boxSmartPlugVendor;
    Boolean boxSmartPlugEnabled;
}
