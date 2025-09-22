package com.telas.controllers;

import com.telas.dtos.request.BoxRequestDto;
import com.telas.dtos.request.StatusBoxMonitorsRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@Tag(name = "Boxes", description = "Endpoints to manage monitors boxes")
public interface BoxController {
    @Operation(summary = "Endpoint to find all boxes", responses = {
            @ApiResponse(responseCode = "200", description = "Boxes found successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Forbidden."),
    })
    ResponseEntity<?> findAll();

    @Operation(summary = "Endpoint to fetch available mac addresses", responses = {
            @ApiResponse(responseCode = "200", description = "Success."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Forbidden."),
    })
    ResponseEntity<?> findAllAvailableAddresses();

    @Operation(summary = "Endpoint to create a box", responses = {
            @ApiResponse(responseCode = "201", description = "Box created successfully."),
            @ApiResponse(responseCode = "422", description = "Request with invalid data."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Forbidden."),
            @ApiResponse(responseCode = "404", description = "Some data not found."),
    })
    ResponseEntity<?> save(BoxRequestDto request);

    @Operation(summary = "Endpoint contract to update a box", responses = {
            @ApiResponse(responseCode = "200", description = "Box updated successfully."),
            @ApiResponse(responseCode = "422", description = "Request with invalid data."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Forbidden."),
            @ApiResponse(responseCode = "404", description = "Box not found."),
    })
    ResponseEntity<?> update(@Valid BoxRequestDto request, UUID boxId);

    @Operation(summary = "Endpoint to fetch monitors and ads data by Ip or MacAddress, this endpoint should be accessed only by Box API", responses = {
            @ApiResponse(responseCode = "200", description = "Monitors and ads founded successfully."),
            @ApiResponse(responseCode = "422", description = "Request with invalid data."),
            @ApiResponse(responseCode = "404", description = "Some data not found."),
    })
    ResponseEntity<?> getMonitorsAdsByIp(@RequestHeader("X-Box-Address") String address);

    @Operation(summary = "Endpoint to update box and monitors health, this endpoint should be accessed only by Box API", responses = {
            @ApiResponse(responseCode = "204", description = "Monitors health checked successfully."),
            @ApiResponse(responseCode = "404", description = "Some data not found."),
            @ApiResponse(responseCode = "500", description = "Internal server error."),
    })
    ResponseEntity<?> updateHealth(@RequestBody StatusBoxMonitorsRequestDto request);
}
