package com.telas.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(name = "Subscriptions", description = "Endpoints to manage subscriptions")
public interface SubscriptionController {
  @Operation(summary = "Endpoint contract to save a subscription", responses = {
          @ApiResponse(responseCode = "201", description = "Subscription created successfully."),
          @ApiResponse(responseCode = "400", description = "Request with invalid data or payment error."),
          @ApiResponse(responseCode = "401", description = "Unauthorized."),
          @ApiResponse(responseCode = "404", description = "Some data not found."),
  })
  ResponseEntity<?> save();

  @Operation(summary = "Endpoint contract to get a subscription by id", responses = {
          @ApiResponse(responseCode = "200", description = "Subscription founded successfully."),
          @ApiResponse(responseCode = "401", description = "Unauthorized."),
          @ApiResponse(responseCode = "403", description = "Forbidden."),
          @ApiResponse(responseCode = "404", description = "Subscription not found."),
  })
  ResponseEntity<?> findById(UUID subscriptionId);
}
