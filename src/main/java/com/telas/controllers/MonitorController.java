package com.telas.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telas.dtos.request.MonitorRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.UUID;

@Tag(name = "Monitors", description = "Endpoints to manage monitors")
public interface MonitorController {
  @Operation(summary = "Endpoint contract to save a monitor", responses = {
          @ApiResponse(responseCode = "201", description = "Monitor created successfully."),
          @ApiResponse(responseCode = "422", description = "Request with invalid data."),
          @ApiResponse(responseCode = "401", description = "Unauthorized."),
          @ApiResponse(responseCode = "403", description = "Forbidden."),
          @ApiResponse(responseCode = "404", description = "Some data not found."),
  })
  ResponseEntity<?> save(@RequestBody MonitorRequestDto request) throws JsonProcessingException;

  @Operation(summary = "Endpoint contract to update a monitor", responses = {
          @ApiResponse(responseCode = "200", description = "Monitor updated successfully."),
          @ApiResponse(responseCode = "422", description = "Request with invalid data."),
          @ApiResponse(responseCode = "401", description = "Unauthorized."),
          @ApiResponse(responseCode = "403", description = "Forbidden."),
          @ApiResponse(responseCode = "404", description = "Some data not found."),
  })
  ResponseEntity<?> update(@RequestBody MonitorRequestDto request, UUID monitorId) throws JsonProcessingException;

  @Operation(summary = "Endpoint contract to get a monitor by id", responses = {
          @ApiResponse(responseCode = "200", description = "Monitor found successfully."),
          @ApiResponse(responseCode = "401", description = "Unauthorized."),
          @ApiResponse(responseCode = "403", description = "Forbidden."),
          @ApiResponse(responseCode = "404", description = "Monitor not found."),
  })
  ResponseEntity<?> findById(UUID monitorId);

  @Operation(summary = "Endpoint contract to find valid ads from monitor", responses = {
          @ApiResponse(responseCode = "200", description = "Ads founded successfully."),
          @ApiResponse(responseCode = "401", description = "Unauthorized."),
          @ApiResponse(responseCode = "403", description = "Forbidden."),
  })
  ResponseEntity<?> findValidAdsForMonitor(UUID monitorId);

  @Operation(summary = "Endpoint contract to get a list of the nearest active monitors from a list of zipCodes", responses = {
          @ApiResponse(responseCode = "200", description = "Monitors founded successfully.")
  })
  ResponseEntity<?> findNearestActiveMonitors(String zipCode, BigDecimal size, String type, int limit);
}
