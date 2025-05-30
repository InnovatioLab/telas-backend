package com.telas.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telas.dtos.request.MonitorRequestDto;
import com.telas.dtos.response.MonitorMinResponseDto;
import com.telas.dtos.response.MonitorResponseDto;
import com.telas.dtos.response.MonitorValidationResponseDto;
import com.telas.entities.Monitor;
import com.telas.entities.Subscription;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface MonitorService {
  void save(MonitorRequestDto requestDto, UUID monitorId) throws JsonProcessingException;

  MonitorResponseDto findById(UUID monitorId);

  Monitor findEntityById(UUID monitorId);

  Map<String, List<MonitorMinResponseDto>> findNearestActiveMonitors(String zipCodes, BigDecimal sizeFilter, String typeFilter, int limit);

  List<Monitor> findAllByIds(List<UUID> monitorIds);

  List<MonitorValidationResponseDto> findInvalidMonitorsOrLinkedAds(List<UUID> monitorIds, UUID clientId);

  void removeMonitorAdsFromSubscription(Subscription subscription);
}
