package com.telas.controllers;

import com.telas.dtos.request.UpdatePaymentStatusRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Payments", description = "Endpoints to manage payments")
public interface PaymentController {
  @Operation(summary = "Endpoint contract to update payment status", responses = {
          @ApiResponse(responseCode = "200", description = "Payment updated successfully."),
          @ApiResponse(responseCode = "401", description = "Unauthorized."),
          @ApiResponse(responseCode = "403", description = "Forbidden."),
          @ApiResponse(responseCode = "404", description = "Some data not found."),
  })
  ResponseEntity<?> updateStatus(UpdatePaymentStatusRequestDto requestDto);
}
