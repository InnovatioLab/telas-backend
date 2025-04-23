package com.marketingproject.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.marketingproject.dtos.request.AdvertisingAttachmentRequestDto;
import com.marketingproject.dtos.request.AttachmentRequestDto;
import com.marketingproject.dtos.request.ClientRequestDto;
import com.marketingproject.dtos.request.ContactRequestDto;
import com.marketingproject.entities.Client;
import com.marketingproject.infra.security.model.AuthenticatedUser;
import com.marketingproject.infra.security.model.PasswordRequestDto;
import com.marketingproject.infra.security.model.PasswordUpdateRequestDto;

import java.util.List;
import java.util.UUID;

public interface ClientService {
    void save(ClientRequestDto request);

    Client findById(UUID id);

    Client findActiveByIdentification(String identification);

    void validateCode(String identification, String codigo);

    void resendCode(String identification);

    void updateContact(String identification, ContactRequestDto request) throws JsonProcessingException;

    void createPassword(String identification, PasswordRequestDto request);

    void sendResetPasswordCode(String identification);

    void resetPassword(String identificationNumber, PasswordRequestDto request);

    void updatePassword(PasswordUpdateRequestDto request, AuthenticatedUser authClient);

    void update(ClientRequestDto request, UUID id) throws JsonProcessingException;

    void uploadAttachments(List<AttachmentRequestDto> request, UUID clientId) throws JsonProcessingException;

    void uploadAdvertisingAttachments(List<AdvertisingAttachmentRequestDto> request, UUID clientId) throws JsonProcessingException;
}
