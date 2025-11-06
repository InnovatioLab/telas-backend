package com.telas.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Addresses", description = "Endpoints to manage addresses")
public interface AddressController {
    @Operation(summary = "Endpoint contract to get an address from zipCode", responses = {
            @ApiResponse(responseCode = "200", description = "Success.")
    })
    ResponseEntity<?> findByZipCode(String zipCode);
}
