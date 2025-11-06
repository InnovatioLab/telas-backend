package com.telas.controllers;

import com.telas.dtos.request.CartRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(name = "Carts", description = "Endpoints to manage carts")
public interface CartController {
  @Operation(summary = "Route contract to save a cart", responses = {
          @ApiResponse(responseCode = "201", description = "Cart successfully created."),
          @ApiResponse(responseCode = "401", description = "Authentication failed."),
          @ApiResponse(responseCode = "403", description = "No permission to perform this operation."),
          @ApiResponse(responseCode = "422", description = "Request with invalid data.")
  })
  ResponseEntity<?> save(CartRequestDto request);

  @Operation(summary = "Route contract to update a cart", responses = {
          @ApiResponse(responseCode = "200", description = "Cart successfully updated."),
          @ApiResponse(responseCode = "422", description = "Request with invalid data."),
          @ApiResponse(responseCode = "401", description = "Authentication failed."),
          @ApiResponse(responseCode = "403", description = "No permission to perform this operation."),
          @ApiResponse(responseCode = "404", description = "Cart not found.")
  })
  ResponseEntity<?> update(CartRequestDto request, UUID cartId);

  @Operation(summary = "Route contract to find a cart by ID", responses = {
          @ApiResponse(responseCode = "200", description = "Cart successfully found."),
          @ApiResponse(responseCode = "401", description = "Authentication failed."),
          @ApiResponse(responseCode = "403", description = "No permission to perform this operation."),
          @ApiResponse(responseCode = "404", description = "Cart not found.")
  })
  ResponseEntity<?> findById(UUID id);

  @Operation(summary = "Route contract to find logged user cart", responses = {
          @ApiResponse(responseCode = "200", description = "Cart successfully found."),
          @ApiResponse(responseCode = "401", description = "Authentication failed."),
          @ApiResponse(responseCode = "403", description = "No permission to perform this operation.")
  })
  ResponseEntity<?> getLoggedUserCart();
}
