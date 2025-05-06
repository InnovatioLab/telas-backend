package com.marketingproject.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.marketingproject.dtos.request.*;
import com.marketingproject.dtos.request.filters.ClientFilterRequestDto;
import com.marketingproject.dtos.request.filters.FilterPendingAttachmentRequestDto;
import com.marketingproject.dtos.response.AttachmentPendingResponseDto;
import com.marketingproject.dtos.response.ClientMinResponseDto;
import com.marketingproject.dtos.response.ClientResponseDto;
import com.marketingproject.dtos.response.PaginationResponseDto;
import com.marketingproject.entities.Client;
import com.marketingproject.enums.AttachmentValidationType;
import com.marketingproject.infra.security.model.AuthenticatedUser;
import com.marketingproject.infra.security.model.PasswordRequestDto;
import com.marketingproject.infra.security.model.PasswordUpdateRequestDto;

import java.util.List;
import java.util.UUID;

public interface ClientService {
    void save(ClientRequestDto request);

    ClientResponseDto findById(UUID id);

    Client findActiveEntityById(UUID id);

    Client findEntityById(UUID id);

    ClientResponseDto getDataFromToken();

    void validateCode(String identification, String codigo);

    void resendCode(String identification);

    void updateContact(String identification, ContactRequestDto request) throws JsonProcessingException;

    void createPassword(String identification, PasswordRequestDto request);

    void sendResetPasswordCode(String identification);

    void resetPassword(String identificationNumber, PasswordRequestDto request);

    void updatePassword(PasswordUpdateRequestDto request, AuthenticatedUser authClient);

    void update(ClientRequestDto request, UUID id) throws JsonProcessingException;

    void uploadAttachments(List<AttachmentRequestDto> request, UUID clientId);

    void uploadAdvertisingAttachments(List<AdvertisingAttachmentRequestDto> request, UUID clientId) throws JsonProcessingException;

    void acceptTermsAndConditions();

    void changeRoleToPartner(UUID clientId) throws JsonProcessingException;

    void validateAttachment(UUID attachmentId, AttachmentValidationType validation, RefusedAttachmentRequestDto request) throws JsonProcessingException;

    PaginationResponseDto<List<ClientMinResponseDto>> findAllFilters(ClientFilterRequestDto request);

    PaginationResponseDto<List<AttachmentPendingResponseDto>> findPendingAttachmentsByFilter(FilterPendingAttachmentRequestDto request);
}
