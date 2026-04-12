package com.telas.services;

import com.telas.dtos.response.BoxHeartbeatCheckResponseDto;
import com.telas.dtos.response.MonitoringTestingRowDto;

import java.util.List;
import java.util.UUID;

public interface MonitoringTestingService {

    List<MonitoringTestingRowDto> getOverview();

    BoxHeartbeatCheckResponseDto checkBoxHeartbeat(UUID boxId);
}
