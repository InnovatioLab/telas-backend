package com.telas.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Terms_Conditions", description = "Endpoints to manage terms")
public interface TermConditionController {

  @Operation(summary = "Endpoint get terms", responses = {
          @ApiResponse(responseCode = "201", description = "Monitor created successfully."),
          @ApiResponse(responseCode = "422", description = "Request with invalid data."),
          @ApiResponse(responseCode = "401", description = "Unauthorized."),
          @ApiResponse(responseCode = "403", description = "Forbidden."),
          @ApiResponse(responseCode = "404", description = "Some data not found."),
  })
  ResponseEntity<?> getTerms();
}
