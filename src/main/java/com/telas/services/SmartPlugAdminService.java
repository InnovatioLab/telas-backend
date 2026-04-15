package com.telas.services;

import com.telas.dtos.request.SmartPlugInventoryRequestDto;
import com.telas.dtos.request.SmartPlugRequestDto;
import com.telas.dtos.request.SmartPlugUpdateRequestDto;
import com.telas.dtos.response.SmartPlugReadingResponseDto;
import com.telas.dtos.response.SmartPlugResponseDto;

import java.util.List;
import java.util.UUID;

public interface SmartPlugAdminService {

    List<SmartPlugResponseDto> findAll();

    List<SmartPlugResponseDto> findUnassignedInventory(UUID forMonitorId, UUID forBoxId);

    SmartPlugResponseDto create(SmartPlugRequestDto dto);

    SmartPlugResponseDto createInventory(SmartPlugInventoryRequestDto dto);

    SmartPlugResponseDto assignToMonitor(UUID plugId, UUID monitorId);

    SmartPlugResponseDto assignToBox(UUID plugId, UUID boxId);

    SmartPlugResponseDto unassign(UUID plugId);

    SmartPlugResponseDto update(UUID id, SmartPlugUpdateRequestDto dto);

    void delete(UUID id);

    SmartPlugReadingResponseDto testRead(UUID id);
}
