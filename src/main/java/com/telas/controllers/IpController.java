package com.telas.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Ips", description = "Endpoints to manage ips")
public interface IpController {
  @Operation(summary = "Endpoint to fetch network available fixed ips", responses = {
          @ApiResponse(responseCode = "200", description = "Success."),
          @ApiResponse(responseCode = "401", description = "Unauthorized."),
          @ApiResponse(responseCode = "403", description = "Forbidden."),
  })
  ResponseEntity<?> findAll();
}
