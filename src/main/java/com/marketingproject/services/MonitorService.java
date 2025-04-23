package com.marketingproject.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.marketingproject.dtos.request.MonitorRequestDto;
import com.marketingproject.entities.Monitor;

import java.util.UUID;

public interface MonitorService {
    void save(MonitorRequestDto requestDto, UUID monitorId) throws JsonProcessingException;

    Monitor findById(UUID monitorId);
}
