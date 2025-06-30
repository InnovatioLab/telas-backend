package com.telas.controllers.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telas.controllers.MonitorController;
import com.telas.dtos.request.MonitorRequestDto;
import com.telas.dtos.request.filters.FilterMonitorRequestDto;
import com.telas.dtos.response.MonitorMinResponseDto;
import com.telas.dtos.response.MonitorResponseDto;
import com.telas.dtos.response.PaginationResponseDto;
import com.telas.dtos.response.ResponseDto;
import com.telas.services.MonitorService;
import com.telas.shared.constants.MessageCommonsConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "monitors")
@RequiredArgsConstructor
public class MonitorControllerImpl implements MonitorController {
  private final MonitorService service;

  @Override
  @PostMapping
  @SecurityRequirement(name = "jwt")
  public ResponseEntity<?> save(@Valid @RequestBody MonitorRequestDto request) throws JsonProcessingException {
    service.save(request, null);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ResponseDto.fromData(null, HttpStatus.CREATED, MessageCommonsConstants.SAVE_SUCCESS_MESSAGE));
  }

  @Override
  @PutMapping("/{id}")
  @SecurityRequirement(name = "jwt")
  public ResponseEntity<?> update(@Valid @RequestBody MonitorRequestDto request, @PathVariable(name = "id") UUID monitorId) throws JsonProcessingException {
    service.save(request, monitorId);
    return ResponseEntity.status(HttpStatus.OK)
            .body(ResponseDto.fromData(null, HttpStatus.OK, MessageCommonsConstants.UPDATE_SUCCESS_MESSAGE));
  }

  @Override
  @GetMapping("/{id}")
  @SecurityRequirement(name = "jwt")
  public ResponseEntity<?> findById(@PathVariable(name = "id") UUID monitorId) {
    return ResponseEntity.status(HttpStatus.OK)
            .body(ResponseDto.fromData(service.findById(monitorId), HttpStatus.OK, MessageCommonsConstants.FIND_ID_SUCCESS_MESSAGE));
  }

  @Override
  @GetMapping("/valid-ads/{id}")
  @SecurityRequirement(name = "jwt")
  public ResponseEntity<?> findValidAdsForMonitor(@PathVariable(name = "id") UUID monitorId) {
    return ResponseEntity.status(HttpStatus.OK)
            .body(ResponseDto.fromData(service.findValidAdsForMonitor(monitorId), HttpStatus.OK, MessageCommonsConstants.FIND_ID_SUCCESS_MESSAGE));
  }

  @Override
  @GetMapping("/filters")
  @SecurityRequirement(name = "jwt")
  public ResponseEntity<?> findAllMonitorsFilters(FilterMonitorRequestDto request) {
    PaginationResponseDto<List<MonitorResponseDto>> response = service.findAllByFilters(request);

    String msg = response.getList().isEmpty() ? MessageCommonsConstants.FIND_FILTER_EMPTY_MESSAGE : MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE;

    return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.fromData(response, HttpStatus.OK, msg));
  }

  @Override
  @GetMapping("/nearest")
  public ResponseEntity<?> findNearestActiveMonitors(
          @RequestParam String zipCodes,
          @RequestParam(required = false) BigDecimal size,
          @RequestParam(required = false) String type,
          @RequestParam(defaultValue = "3") int limit) {
    Map<String, List<MonitorMinResponseDto>> monitors = service.findNearestActiveMonitors(zipCodes, size, type, limit);
    String message = monitors.isEmpty() ? MessageCommonsConstants.FIND_FILTER_EMPTY_MESSAGE : MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE;
    return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.fromData(monitors, HttpStatus.OK, message));
  }
}
