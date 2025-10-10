package com.telas.infra.security.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telas.dtos.response.ResponseDto;
import com.telas.infra.security.model.LoginRequestDto;
import com.telas.infra.security.model.PasswordRequestDto;
import com.telas.infra.security.model.PasswordUpdateRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Auth", description = "Operations for user authentication")
public interface AuthController {
    @Operation(summary = "Endpoint for user login", responses = {
            @ApiResponse(responseCode = "201", description = "Login successful."),
            @ApiResponse(responseCode = "422", description = "Invalid request data."),
            @ApiResponse(responseCode = "401", description = "Authentication failed."),
            @ApiResponse(responseCode = "404", description = "User not found."),
            @ApiResponse(responseCode = "500", description = "Internal server error.")
    })
    ResponseEntity<ResponseDto<String>> login(@Valid @RequestBody LoginRequestDto loginRequestDto) throws JsonProcessingException;

    @Operation(summary = "Endpoint to obtain the password recovery code, also used for resending the code", responses = {
            @ApiResponse(responseCode = "201", description = "Recovery code sent successfully."),
            @ApiResponse(responseCode = "422", description = "Invalid request data."),
            @ApiResponse(responseCode = "404", description = "User not found.")
    })
    ResponseEntity<?> sendPasswordRecoveryCode(@PathVariable(name = "email") String email);

    @Operation(summary = "Endpoint to reset the user's password", responses = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully."),
            @ApiResponse(responseCode = "422", description = "Invalid request data."),
            @ApiResponse(responseCode = "404", description = "User not found.")
    })
    ResponseEntity<?> resetPassword(@PathVariable(name = "email") String email, @Valid @RequestBody PasswordRequestDto request);

    @Operation(summary = "Endpoint to update the user's password", responses = {
            @ApiResponse(responseCode = "200", description = "Password updated successfully."),
            @ApiResponse(responseCode = "422", description = "Invalid request data."),
            @ApiResponse(responseCode = "401", description = "Authentication failed."),
            @ApiResponse(responseCode = "403", description = "No permission to perform this operation."),
            @ApiResponse(responseCode = "404", description = "User not found.")
    })
    ResponseEntity<?> updatePassword(@Valid @RequestBody PasswordUpdateRequestDto request);
}

