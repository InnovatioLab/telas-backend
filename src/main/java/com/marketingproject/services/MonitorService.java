package com.marketingproject.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.marketingproject.dtos.request.MonitorRequestDto;
import com.marketingproject.dtos.response.MonitorMinResponseDto;
import com.marketingproject.dtos.response.MonitorResponseDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface MonitorService {
    void save(MonitorRequestDto requestDto, UUID monitorId) throws JsonProcessingException;

    MonitorResponseDto findById(UUID monitorId);

    List<MonitorMinResponseDto> findNearestActiveMonitors(String zipCode, BigDecimal sizeFilter, String typeFilter, int limit);
}
