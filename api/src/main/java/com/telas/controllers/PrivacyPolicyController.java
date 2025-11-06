package com.telas.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Privacy Policy", description = "Endpoints to manage privacy policy")
public interface PrivacyPolicyController {
  @Operation(summary = "Endpoint get latest privacy policy", responses = {
          @ApiResponse(responseCode = "200", description = "Success.")
  })
  ResponseEntity<?> getPolicyPrivacy();
}
