package com.telas.services;

import com.telas.dtos.request.AttachmentRequestDto;
import com.telas.dtos.request.ClientAdRequestToAdminDto;
import com.telas.dtos.request.ClientRequestDto;
import com.telas.dtos.request.RefusedAdRequestDto;
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

    ClientResponseDto findByEmailUnprotected(String email);

    Client findActiveEntityById(UUID id);

    Client findEntityById(UUID id);

    ClientResponseDto getDataFromToken();

    void validateCode(String email, String codigo);

    void resendCode(String email);

    void createPassword(String email, PasswordRequestDto request);

    void sendResetPasswordCode(String email);

    void resetPassword(String email, PasswordRequestDto request);

    void updatePassword(PasswordUpdateRequestDto request, AuthenticatedUser authClient);

    void update(ClientRequestDto request, UUID id);

    void uploadAttachments(List<AttachmentRequestDto> request);

    void requestAdCreation(ClientAdRequestToAdminDto request);

    void uploadAds(AttachmentRequestDto request, UUID clientId);

    void acceptTermsAndConditions();

    void changeRoleToPartner(UUID clientId);

    void validateAd(UUID adId, AdValidationType validation, RefusedAdRequestDto request);

    void incrementSubscriptionFlow();

    PaginationResponseDto<List<ClientMinResponseDto>> findAllFilters(ClientFilterRequestDto request);

    PaginationResponseDto<List<AdRequestAdminResponseDto>> findPendingAdRequest(FilterAdRequestDto request);

    void addMonitorToWishlist(UUID monitorId);

    WishlistResponseDto getWishlistMonitors();
}
