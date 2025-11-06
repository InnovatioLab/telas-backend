package com.telas.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Terms Conditions", description = "Endpoints to manage terms")
public interface TermConditionController {

  @Operation(summary = "Endpoint get terms", responses = {
          @ApiResponse(responseCode = "200", description = "Success.")
  })
  ResponseEntity<?> getTerms();
}
