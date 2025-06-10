package com.telas.controllers;

import com.telas.dtos.request.filters.SubscriptionFilterRequestDto;
import com.telas.enums.Recurrence;
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

  @Operation(summary = "Endpoint contract to filter and list subscriptions of the logged client", responses = {
          @ApiResponse(responseCode = "200", description = "Subscriptions filtered successfully."),
          @ApiResponse(responseCode = "401", description = "Unauthorized."),
          @ApiResponse(responseCode = "403", description = "Forbidden."),
  })
  ResponseEntity<?> findClientSubscriptionsFilters(SubscriptionFilterRequestDto request);

  @Operation(summary = "Endpoint contract to get a subscription by id", responses = {
          @ApiResponse(responseCode = "200", description = "Subscription founded successfully."),
          @ApiResponse(responseCode = "401", description = "Unauthorized."),
          @ApiResponse(responseCode = "403", description = "Forbidden."),
          @ApiResponse(responseCode = "404", description = "Subscription not found."),
  })
  ResponseEntity<?> findById(UUID subscriptionId);

  @Operation(summary = "Endpoint contract to upgrade a one time buy subscription by id", responses = {
          @ApiResponse(responseCode = "200", description = "Subscription upgraded successfully."),
          @ApiResponse(responseCode = "400", description = "Request with invalid data or payment error."),
          @ApiResponse(responseCode = "401", description = "Unauthorized."),
          @ApiResponse(responseCode = "403", description = "Forbidden."),
          @ApiResponse(responseCode = "404", description = "Subscription not found."),
  })
  ResponseEntity<?> upgradeSubscription(UUID subscriptionId, Recurrence recurrence);

  @Operation(summary = "Endpoint contract to cancel a monthly and active subscription by id", responses = {
          @ApiResponse(responseCode = "204", description = "Subscription cancelled successfully."),
          @ApiResponse(responseCode = "401", description = "Unauthorized."),
          @ApiResponse(responseCode = "403", description = "Forbidden."),
          @ApiResponse(responseCode = "404", description = "Subscription not found."),
  })
  ResponseEntity<?> cancelSubscription(UUID subscriptionId);
}
