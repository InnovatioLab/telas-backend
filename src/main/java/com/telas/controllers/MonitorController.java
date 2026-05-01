package com.telas.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telas.dtos.request.AttachmentRequestDto;
import com.telas.dtos.request.MonitorRequestDto;
import com.telas.dtos.request.filters.FilterMonitorRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

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
            @ApiResponse(responseCode = "404", description = "Monitor not found."),
    })
    ResponseEntity<?> findValidAdsForMonitor(UUID monitorId, String name);

    @Operation(summary = "Endpoint contract to find all monitors paginated with filters", responses = {
            @ApiResponse(responseCode = "200", description = "Monitors founded successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Forbidden."),
    })
    ResponseEntity<?> findAllMonitorsFilters(FilterMonitorRequestDto request);

    @Operation(summary = "Endpoint contract to find all monitors", responses = {
            @ApiResponse(responseCode = "200", description = "Monitors founded successfully.")
    })
    ResponseEntity<?> findAllMonitors();

    @Operation(summary = "Endpoint contract to get a list of valid active monitors from a zipCode", responses = {
            @ApiResponse(responseCode = "200", description = "Monitors founded successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
    })
    ResponseEntity<?> findValidByZipCode(String zipCode);

    @Operation(summary = "Available monitors inside a geographic rectangle (max 0.5 deg per axis, authenticated client)", responses = {
            @ApiResponse(responseCode = "200", description = "Monitors founded successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "422", description = "Bounds invalid or too large."),
    })
    ResponseEntity<?> findMonitorsInViewport(double minLat, double maxLat, double minLng, double maxLng);

    @Operation(summary = "Admin: monitors in ZIP including inactive (map health)", responses = {
            @ApiResponse(responseCode = "200", description = "Monitors founded successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Forbidden."),
    })
    ResponseEntity<?> findMonitorsForAdminMapByZipCode(String zipCode);

    @Operation(summary = "Endpoint contract to get from the box, the list of current displayed ads from monitor", responses = {
            @ApiResponse(responseCode = "200", description = "Ads founded successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Forbidden."),
            @ApiResponse(responseCode = "404", description = "Monitor not found."),
    })
    ResponseEntity<?> findCurrentDisplayedAdsFromBox(UUID monitorId);

    @Operation(summary = "Endpoint contract to delete a monitor", responses = {
            @ApiResponse(responseCode = "204", description = "Monitor deleted successfully."),
            @ApiResponse(responseCode = "422", description = "Request with invalid data."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Forbidden."),
            @ApiResponse(responseCode = "404", description = "Monitor not found."),
    })
    ResponseEntity<?> delete(UUID monitorId);

    @Operation(summary = "Admin: upload and attach an ad directly to a monitor", responses = {
            @ApiResponse(responseCode = "201", description = "Created."),
            @ApiResponse(responseCode = "422", description = "Request with invalid data."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Forbidden."),
            @ApiResponse(responseCode = "404", description = "Monitor not found."),
    })
    ResponseEntity<?> uploadDirectAdToMonitor(UUID monitorId, AttachmentRequestDto request);

    @Operation(summary = "Admin: remove an available ad (delete file and record)", responses = {
            @ApiResponse(responseCode = "204", description = "Deleted."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Forbidden."),
            @ApiResponse(responseCode = "404", description = "Not found."),
            @ApiResponse(responseCode = "422", description = "Invalid request."),
    })
    ResponseEntity<?> deleteAvailableAd(UUID monitorId, UUID adId);
}
