package com.marketingproject.controllers;

import com.marketingproject.dtos.request.ClientRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Clients", description = "Endpoints to manage clients")
public interface ClientController {
    @Operation(summary = "Endpoint contract to save a client", responses = {
            @ApiResponse(responseCode = "201", description = "Client created successfully."),
            @ApiResponse(responseCode = "400", description = "Request with invalid data."),
    })
    ResponseEntity<?> save(ClientRequestDto request);
}
