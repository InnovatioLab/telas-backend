package com.telas.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telas.dtos.request.MonitorRequestDto;
import com.telas.dtos.request.filters.FilterMonitorRequestDto;
import com.telas.dtos.response.*;
import com.telas.entities.Monitor;
import com.telas.entities.Subscription;

import java.util.List;
import java.util.UUID;

public interface MonitorService {
  void save(MonitorRequestDto requestDto, UUID monitorId) throws JsonProcessingException;

  void removeMonitorAdsFromSubscription(Subscription subscription);

  MonitorResponseDto findById(UUID monitorId);

  Monitor findEntityById(UUID monitorId);

  List<MonitorMapsResponseDto> findNearestActiveMonitors(String zipCode);

  List<MonitorBoxMinResponseDto> findAllMonitors();

  List<Monitor> findAllByIds(List<UUID> monitorIds);

  List<MonitorValidAdResponseDto> findValidAdsForMonitor(UUID monitorId);

  PaginationResponseDto<List<MonitorResponseDto>> findAllByFilters(FilterMonitorRequestDto request);

  void delete(UUID monitorId);

  List<MonitorValidAdResponseDto> findCurrentDisplayedAdsFromBox(UUID monitorId);
}
