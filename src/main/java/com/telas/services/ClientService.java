package com.telas.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telas.dtos.request.*;
import com.telas.dtos.request.filters.ClientFilterRequestDto;
import com.telas.dtos.request.filters.FilterAdRequestDto;
import com.telas.dtos.response.*;
import com.telas.entities.Client;
import com.telas.enums.AdValidationType;
import com.telas.infra.security.model.AuthenticatedUser;
import com.telas.infra.security.model.PasswordRequestDto;
import com.telas.infra.security.model.PasswordUpdateRequestDto;

import java.util.List;
import java.util.UUID;

public interface ClientService {
    void save(ClientRequestDto request);

    ClientResponseDto findById(UUID id);

    ClientResponseDto findByIdentificationNumber(String identification);

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

    void requestAdCreation(ClientAdRequestToAdminDto request);

    void uploadAds(AdRequestDto request);

    void acceptTermsAndConditions();

    void changeRoleToPartner(UUID clientId) throws JsonProcessingException;

    List<AdResponseDto> findPendingAds();

    void validateAd(UUID adId, AdValidationType validation, RefusedAdRequestDto request) throws JsonProcessingException;

    PaginationResponseDto<List<ClientMinResponseDto>> findAllFilters(ClientFilterRequestDto request);

    PaginationResponseDto<List<AdRequestResponseDto>> findPendingAdRequest(FilterAdRequestDto request);
}
