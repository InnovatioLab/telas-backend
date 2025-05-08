package com.telas.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telas.dtos.request.MonitorRequestDto;
import com.telas.dtos.response.MonitorMinResponseDto;
import com.telas.dtos.response.MonitorResponseDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface MonitorService {
    void save(MonitorRequestDto requestDto, UUID monitorId) throws JsonProcessingException;

    MonitorResponseDto findById(UUID monitorId);

    Map<String, List<MonitorMinResponseDto>> findNearestActiveMonitors(String zipCodes, BigDecimal sizeFilter, String typeFilter, int limit);
}
