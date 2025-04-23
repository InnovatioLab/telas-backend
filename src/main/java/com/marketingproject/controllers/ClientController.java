package com.marketingproject.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.marketingproject.dtos.request.AdvertisingAttachmentRequestDto;
import com.marketingproject.dtos.request.AttachmentRequestDto;
import com.marketingproject.dtos.request.ClientRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@Tag(name = "Clients", description = "Endpoints to manage clients")
public interface ClientController {
    @Operation(summary = "Endpoint contract to save a client", responses = {
            @ApiResponse(responseCode = "201", description = "Client created successfully."),
            @ApiResponse(responseCode = "400", description = "Request with invalid data."),
    })
    ResponseEntity<?> save(@Valid ClientRequestDto request);

    @Operation(summary = "Endpoint contract to update an user", responses = {
            @ApiResponse(responseCode = "200", description = "Client updated successfully."),
            @ApiResponse(responseCode = "400", description = "Request with invalid data."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Forbidden."),
            @ApiResponse(responseCode = "404", description = "Client not found."),
    })
    ResponseEntity<?> update(@Valid ClientRequestDto request, UUID clientId) throws JsonProcessingException;

    @Operation(summary = "Endpoint contract to save or update attachments of an user", responses = {
            @ApiResponse(responseCode = "201", description = "Attachment created/updated successfully."),
            @ApiResponse(responseCode = "400", description = "Request with invalid data."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Forbidden."),
            @ApiResponse(responseCode = "404", description = "Some data not found."),
    })
    ResponseEntity<?> uploadAttachments(@Valid List<AttachmentRequestDto> request, UUID clientId) throws JsonProcessingException;

    @Operation(summary = "Endpoint contract to save or update AdvertisingAttachment of an user", responses = {
            @ApiResponse(responseCode = "201", description = "AdvertisingAttachment created/updated successfully."),
            @ApiResponse(responseCode = "400", description = "Request with invalid data."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Forbidden."),
            @ApiResponse(responseCode = "404", description = "Some data not found."),
    })
    ResponseEntity<?> uploadAdvertisingAttachments(@Valid List<AdvertisingAttachmentRequestDto> request, UUID clientId) throws JsonProcessingException;
}
