package com.telas.infra.security.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telas.dtos.response.ResponseDto;
import com.telas.infra.security.model.LoginRequestDto;
import com.telas.infra.security.model.PasswordRequestDto;
import com.telas.infra.security.model.PasswordUpdateRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Auth", description = "Operations for user authentication")
public interface AuthController {
    @Operation(summary = "Endpoint for user login", responses = {
            @ApiResponse(responseCode = "201", description = "Login successful."),
            @ApiResponse(responseCode = "400", description = "Invalid request data."),
            @ApiResponse(responseCode = "401", description = "Authentication failed."),
            @ApiResponse(responseCode = "404", description = "User not found."),
            @ApiResponse(responseCode = "500", description = "Internal server error.")
    })
    ResponseEntity<ResponseDto<String>> login(LoginRequestDto loginRequestDto) throws JsonProcessingException;

    @Operation(summary = "Endpoint to obtain the password recovery code, also used for resending the code", responses = {
            @ApiResponse(responseCode = "201", description = "Recovery code sent successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid request data."),
            @ApiResponse(responseCode = "404", description = "User not found.")
    })
    ResponseEntity<?> sendPasswordRecoveryCode(String identificationNumber);

    @Operation(summary = "Endpoint to reset the user's password", responses = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid request data."),
            @ApiResponse(responseCode = "404", description = "User not found.")
    })
    ResponseEntity<?> resetPassword(String identificationNumber, PasswordRequestDto request);

    @Operation(summary = "Endpoint to update the user's password", responses = {
            @ApiResponse(responseCode = "200", description = "Password updated successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid request data."),
            @ApiResponse(responseCode = "401", description = "Authentication failed."),
            @ApiResponse(responseCode = "403", description = "No permission to perform this operation."),
            @ApiResponse(responseCode = "404", description = "User not found.")
    })
    ResponseEntity<?> updatePassword(PasswordUpdateRequestDto request);
}

