package com.telas.services.impl;

import com.telas.dtos.request.*;
import com.telas.dtos.request.filters.ClientFilterRequestDto;
import com.telas.dtos.request.filters.FilterAdRequestDto;
import com.telas.dtos.response.*;
import com.telas.entities.Client;
import com.telas.enums.AdValidationType;
import com.telas.infra.security.model.AuthenticatedUser;
import com.telas.infra.security.model.PasswordRequestDto;
import com.telas.infra.security.model.PasswordUpdateRequestDto;
import com.telas.services.*;
import com.telas.shared.model.NamedDownloadResource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientProfileService clientProfileService;
    private final ClientAdRequestService clientAdRequestService;
    private final PartnerPortalService partnerPortalService;
    private final ClientAdminQueryService clientAdminQueryService;

    @Override
    @Transactional
    public void save(ClientRequestDto request) {
        clientProfileService.save(request);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponseDto findById(UUID id) {
        return clientProfileService.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponseDto findByEmailUnprotected(String email) {
        return clientProfileService.findByEmailUnprotected(email);
    }

    @Override
    @Transactional(readOnly = true)
    public Client findActiveEntityById(UUID id) {
        return clientProfileService.findActiveEntityById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Client findEntityById(UUID id) {
        return clientProfileService.findEntityById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponseDto getDataFromToken() {
        return clientProfileService.getDataFromToken();
    }

    @Override
    @Transactional
    public void validateCode(String email, String codigo) {
        clientProfileService.validateCode(email, codigo);
    }

    @Override
    @Transactional
    public void resendCode(String email) {
        clientProfileService.resendCode(email);
    }

    @Override
    @Transactional
    public void createPassword(String email, PasswordRequestDto request) {
        clientProfileService.createPassword(email, request);
    }

    @Override
    @Transactional
    public void sendResetPasswordCode(String email) {
        clientProfileService.sendResetPasswordCode(email);
    }

    @Override
    @Transactional
    public void resetPassword(String email, PasswordRequestDto request) {
        clientProfileService.resetPassword(email, request);
    }

    @Override
    @Transactional
    public void updatePassword(PasswordUpdateRequestDto request, AuthenticatedUser authClient) {
        clientProfileService.updatePassword(request, authClient);
    }

    @Override
    @Transactional
    public void update(ClientRequestDto request, UUID id) {
        clientProfileService.update(request, id);
    }

    @Override
    @Transactional
    public void uploadAttachments(List<AttachmentRequestDto> request) {
        clientProfileService.uploadAttachments(request);
    }

    @Override
    @Transactional
    public void deleteClientAttachment(UUID attachmentId) {
        clientProfileService.deleteClientAttachment(attachmentId);
    }

    @Override
    @Transactional
    public void requestAdCreation(ClientAdRequestToAdminDto request) {
        clientAdRequestService.requestAdCreation(request);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BusinessQuestionnaireAnswersRequestDto> getBusinessQuestionnaireDraft() {
        return clientAdRequestService.getBusinessQuestionnaireDraft();
    }

    @Override
    @Transactional
    public void saveBusinessQuestionnaireDraft(BusinessQuestionnaireAnswersRequestDto answers) {
        clientAdRequestService.saveBusinessQuestionnaireDraft(answers);
    }

    @Override
    @Transactional
    public void updateAdRequestBusinessQuestionnaire(UUID adRequestId, BusinessQuestionnaireAnswersRequestDto answers) {
        clientAdRequestService.updateAdRequestBusinessQuestionnaire(adRequestId, answers);
    }

    @Override
    @Transactional(readOnly = true)
    public NamedDownloadResource exportAdRequestBusinessQuestionnaireTxtAdmin(UUID adRequestId) {
        return clientAdRequestService.exportAdRequestBusinessQuestionnaireTxtAdmin(adRequestId);
    }

    @Override
    @Transactional
    public void uploadAds(AttachmentRequestDto request, UUID clientId) {
        clientAdRequestService.uploadAds(request, clientId);
    }

    @Override
    @Transactional
    public void uploadAdsForAdRequest(AttachmentRequestDto request, UUID adRequestId) {
        clientAdRequestService.uploadAdsForAdRequest(request, adRequestId);
    }

    @Override
    @Transactional
    public void approveAdRequestToAds(UUID adRequestId) {
        clientAdRequestService.approveAdRequestToAds(adRequestId);
    }

    @Override
    @Transactional
    public void cancelAdRequest(UUID adRequestId) {
        clientAdRequestService.cancelAdRequest(adRequestId);
    }

    @Override
    @Transactional
    public void acceptTermsAndConditions() {
        clientProfileService.acceptTermsAndConditions();
    }

    @Override
    @Transactional
    public void changeRoleToPartner(UUID clientId) {
        partnerPortalService.changeRoleToPartner(clientId);
    }

    @Override
    @Transactional
    public ClientMinResponseDto createPartnerByAdmin(CreatePartnerRequestDto request) {
        return partnerPortalService.createPartnerByAdmin(request);
    }

    @Override
    @Transactional
    public void deactivateClientByDeveloper(UUID clientId) {
        clientProfileService.deactivateClientByDeveloper(clientId);
    }

    @Override
    @Transactional
    public void reactivateClientByDeveloper(UUID clientId) {
        clientProfileService.reactivateClientByDeveloper(clientId);
    }

    @Override
    @Transactional
    public void softDeleteClientByDeveloper(UUID clientId) {
        clientProfileService.softDeleteClientByDeveloper(clientId);
    }

    @Override
    @Transactional
    public void restoreSoftDeletedClientByDeveloper(UUID clientId) {
        clientProfileService.restoreSoftDeletedClientByDeveloper(clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public PermanentDeletionRequirementsDto getPermanentDeletionRequirements(UUID clientId) {
        return clientProfileService.getPermanentDeletionRequirements(clientId);
    }

    @Override
    @Transactional
    public void permanentlyDeleteClientByDeveloper(UUID clientId, PermanentDeleteClientRequestDto request) {
        clientProfileService.permanentlyDeleteClientByDeveloper(clientId, request);
    }

    @Override
    @Transactional
    public void validateAd(UUID adId, AdValidationType validation, RefusedAdRequestDto request) {
        clientAdRequestService.validateAd(adId, validation, request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdMessageResponseDto> listAdMessages(UUID adId) {
        return clientAdRequestService.listAdMessages(adId);
    }

    @Override
    @Transactional
    public void sendAdMessage(UUID adId, AdMessageRequestDto request) {
        clientAdRequestService.sendAdMessage(adId, request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminClientMessageRowDto> listClientMessagesHistory(UUID clientId) {
        return clientAdRequestService.listClientMessagesHistory(clientId);
    }

    @Override
    @Transactional
    public void incrementSubscriptionFlow() {
        clientProfileService.incrementSubscriptionFlow();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponseDto<List<ClientMinResponseDto>> findAllFilters(ClientFilterRequestDto request) {
        return clientAdminQueryService.findAllFilters(request);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponseDto<List<AdRequestAdminResponseDto>> findPendingAdRequest(FilterAdRequestDto request) {
        return clientAdminQueryService.findPendingAdRequest(request);
    }

    @Override
    public com.telas.dtos.response.AdRequestMediaResponseDto findAdRequestMedia(UUID adRequestId) {
        return clientAdminQueryService.findAdRequestMedia(adRequestId);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponseDto<List<PendingAdAdminValidationResponseDto>> findPendingAds(FilterAdRequestDto request) {
        return clientAdminQueryService.findPendingAds(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingAdAdminValidationResponseDto> findMyPendingValidationAds() {
        return partnerPortalService.findMyPendingValidationAds();
    }

    @Override
    @Transactional
    public void requestPartnerAdRemoval(UUID adId, PartnerAdRemovalRequestDto request) {
        partnerPortalService.requestPartnerAdRemoval(adId, request);
    }

    @Override
    @Transactional
    public void addMonitorToWishlist(UUID monitorId) {
        partnerPortalService.addMonitorToWishlist(monitorId);
    }

    @Override
    @Transactional(readOnly = true)
    public WishlistResponseDto getWishlistMonitors() {
        return partnerPortalService.getWishlistMonitors();
    }
}
