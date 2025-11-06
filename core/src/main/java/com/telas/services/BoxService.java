package com.telas.services;

import com.telas.dtos.request.BoxRequestDto;
import com.telas.dtos.request.StatusBoxMonitorsRequestDto;
import com.telas.dtos.response.BoxMonitorAdResponseDto;
import com.telas.dtos.response.BoxResponseDto;

import java.util.List;
import java.util.UUID;

public interface BoxService {
    List<BoxResponseDto> findAll();

    void save(BoxRequestDto request, UUID boxId);

    List<BoxMonitorAdResponseDto> getMonitorsAdsByAddress(String address);

    void updateHealth(StatusBoxMonitorsRequestDto request);
}