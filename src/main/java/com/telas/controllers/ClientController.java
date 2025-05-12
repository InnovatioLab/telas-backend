package com.telas.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telas.dtos.request.*;
import com.telas.dtos.request.filters.ClientFilterRequestDto;
import com.telas.dtos.request.filters.FilterAdRequestDto;
import com.telas.enums.AdValidationType;
import com.telas.infra.security.model.PasswordRequestDto;
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

    @Operation(summary = "Endpoint contract to validate the client's code, be it registration confirmation or password recovery", responses = {
            @ApiResponse(responseCode = "200", description = "Code validated successfully."),
            @ApiResponse(responseCode = "400", description = "Request with invalid data."),
            @ApiResponse(responseCode = "404", description = "User not found.")
    })
    ResponseEntity<?> validateCode(String identification, String code);

    @Operation(summary = "Endpoint contract for resending verification code", responses = {
            @ApiResponse(responseCode = "201", description = "Code resent successfully."),
            @ApiResponse(responseCode = "400", description = "Request with invalid data."),
            @ApiResponse(responseCode = "404", description = "User not found.")
    })
    ResponseEntity<?> resendCode(String identification);

    @Operation(summary = "Endpoint contract to change client's primary contact", responses = {
            @ApiResponse(responseCode = "200", description = "Contact changed successfully."),
            @ApiResponse(responseCode = "400", description = "Request with invalid data."),
            @ApiResponse(responseCode = "403", description = "No permission to perform this operation."),
            @ApiResponse(responseCode = "404", description = "User not found.")
    })
    ResponseEntity<?> updateContact(String identification, @Valid ContactRequestDto request) throws JsonProcessingException;

    @Operation(summary = "Endpoint contract to create client password", responses = {
            @ApiResponse(responseCode = "200", description = "Password created successfully."),
            @ApiResponse(responseCode = "400", description = "Request with invalid data. "),
            @ApiResponse(responseCode = "403", description = "No permission to perform this operation."),
            @ApiResponse(responseCode = "404", description = "User not found.")
    })
    ResponseEntity<?> createPassword(String identification, @Valid PasswordRequestDto request);

    @Operation(summary = "Endpoint contract to get client data by id", responses = {
            @ApiResponse(responseCode = "200", description = "Client founded with successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Forbidden."),
            @ApiResponse(responseCode = "404", description = "Client not found."),
    })
    ResponseEntity<?> findById(UUID clientId);

    @Operation(summary = "Endpoint contract to find a client data by identification number", responses = {
            @ApiResponse(responseCode = "200", description = "Client founded with successfully."),
            @ApiResponse(responseCode = "404", description = "Client not found."),
    })
    ResponseEntity<?> findByIdentificationNumber(String identificationNumber);

    @Operation(summary = "Endpoint contract to get client data from the token", responses = {
            @ApiResponse(responseCode = "200", description = "Client founded with successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "404", description = "Client not found."),
    })
    ResponseEntity<?> getDataFromToken();

    @Operation(summary = "Endpoint contract to update a client", responses = {
            @ApiResponse(responseCode = "200", description = "Client updated successfully."),
            @ApiResponse(responseCode = "400", description = "Request with invalid data."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Forbidden."),
            @ApiResponse(responseCode = "404", description = "Client not found."),
    })
    ResponseEntity<?> update(@Valid ClientRequestDto request, UUID clientId) throws JsonProcessingException;

    @Operation(summary = "Endpoint contract to save or update attachments of a client", responses = {
            @ApiResponse(responseCode = "201", description = "Attachment created/updated successfully."),
            @ApiResponse(responseCode = "400", description = "Request with invalid data."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Forbidden."),
            @ApiResponse(responseCode = "404", description = "Some data not found."),
    })
    ResponseEntity<?> uploadAttachments(@Valid List<AttachmentRequestDto> request, UUID clientId) throws JsonProcessingException;

    @Operation(summary = "Endpoint contract to request ad creation to admin", responses = {
            @ApiResponse(responseCode = "201", description = "Ad Request created successfully."),
            @ApiResponse(responseCode = "400", description = "Request with invalid data."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "404", description = "Some attachment not found."),
    })
    ResponseEntity<?> requestAdCreation(@Valid ClientAdRequestToAdminDto request);

    @Operation(summary = "Endpoint contract to save or update an ad of a client", responses = {
            @ApiResponse(responseCode = "201", description = "Ad created/updated successfully."),
            @ApiResponse(responseCode = "400", description = "Request with invalid data."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Forbidden."),
            @ApiResponse(responseCode = "404", description = "Some data not found."),
    })
    ResponseEntity<?> uploadAd(@Valid AdRequestDto request);

    @Operation(summary = "Endpoint contract to get paginated clients from filters", responses = {
            @ApiResponse(responseCode = "200", description = "Records found successfully."),
            @ApiResponse(responseCode = "400", description = "Request with invalid data."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Forbidden.")
    })
    ResponseEntity<?> findAllFilters(ClientFilterRequestDto request);

    @Operation(summary = "Endpoint contract to accept current Terms and Conditions", responses = {
            @ApiResponse(responseCode = "202", description = "Terms and conditions accepted successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    ResponseEntity<?> acceptTermsConditions();

    @Operation(summary = "Endpoint contract to change client role to Partner", responses = {
            @ApiResponse(responseCode = "200", description = "Role changed successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Forbidden."),
            @ApiResponse(responseCode = "404", description = "Client not found."),
    })
    ResponseEntity<?> changeRoleToPartner(UUID clientId) throws JsonProcessingException;

    @Operation(summary = "Endpoint contract to filter and list ads request", responses = {
            @ApiResponse(responseCode = "200", description = "Ads requests filtered successfully."),
            @ApiResponse(responseCode = "400", description = "Erro while filtering ads"),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Forbidden."),
            @ApiResponse(responseCode = "404", description = "Some data not found."),
    })
    ResponseEntity<?> findAdRequestsByFilter(FilterAdRequestDto request);

    @Operation(summary = "Endpoint contract find pending ads from logged user", responses = {
            @ApiResponse(responseCode = "200", description = "Pending ads founded successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Forbidden."),
            @ApiResponse(responseCode = "404", description = "Some data not found."),
    })
    ResponseEntity<?> findPendingAds();

    @Operation(summary = "Endpoint contract to validate an ad", responses = {
            @ApiResponse(responseCode = "200", description = "Ad validated successfully."),
            @ApiResponse(responseCode = "400", description = "Request with invalid data. "),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Forbidden."),
            @ApiResponse(responseCode = "404", description = "Some data not found."),
    })
    ResponseEntity<?> validateAd(AdValidationType validation, RefusedAdRequestDto request, UUID attachmentId) throws JsonProcessingException;
}
