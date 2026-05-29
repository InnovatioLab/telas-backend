package com.telas.services.impl;

import com.telas.dtos.request.AdMessageRequestDto;
import com.telas.dtos.request.AttachmentRequestDto;
import com.telas.dtos.request.BusinessQuestionnaireAnswersRequestDto;
import com.telas.dtos.request.ClientAdRequestToAdminDto;
import com.telas.dtos.request.RefusedAdRequestDto;
import com.telas.dtos.response.AdMessageResponseDto;
import com.telas.dtos.response.AdminClientMessageRowDto;
import com.telas.entities.Ad;
import com.telas.entities.AdMessage;
import com.telas.entities.AdRequest;
import com.telas.entities.Client;
import com.telas.enums.AdRequestOrigin;
import com.telas.enums.AdValidationType;
import com.telas.enums.PartnerSubmissionMode;
import com.telas.enums.Role;
import com.telas.services.ad.AdApprovalWorkflow;
import com.telas.helpers.ClientHelper;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ForbiddenException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.AdMessageRepository;
import com.telas.services.BusinessQuestionnaireService;
import com.telas.services.ClientAdRequestService;
import com.telas.services.ClientProfileService;
import com.telas.services.ad.AdValidationService;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.ClientValidationMessages;
import com.telas.shared.model.NamedDownloadResource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientAdRequestServiceImpl implements ClientAdRequestService {

    private final ClientProfileService clientProfileService;
    private final ClientHelper helper;
    private final AdApprovalWorkflow adApprovalWorkflow;
    private final AdValidationService adValidationService;
    private final AuthenticatedUserService authenticatedUserService;
    private final AdMessageRepository adMessageRepository;
    private final BusinessQuestionnaireService businessQuestionnaireService;

    @Override
    @Transactional
    public void requestAdCreation(ClientAdRequestToAdminDto request) {
        Client client = authenticatedUserService.validateActiveSubscription().client();

        if (Role.ADMIN.equals(client.getRole())) {
            return;
        }

        if (Objects.nonNull(client.getAdRequest())) {
            throw new ForbiddenException(ClientValidationMessages.AD_REQUEST_EXISTS);
        }

        validateMaxAds(client);
        AdRequest created = helper.createAdRequest(request, client);
        businessQuestionnaireService.createQuestionnaireForNewAdRequest(client.getId(), created, request.getBusinessAnswers());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BusinessQuestionnaireAnswersRequestDto> getBusinessQuestionnaireDraft() {
        UUID clientId = authenticatedUserService.validateNonPartner().client().getId();
        return businessQuestionnaireService.getDraftAnswers(clientId);
    }

    @Override
    @Transactional
    public void saveBusinessQuestionnaireDraft(BusinessQuestionnaireAnswersRequestDto answers) {
        UUID clientId = authenticatedUserService.validateNonPartner().client().getId();
        businessQuestionnaireService.saveDraft(clientId, answers);
    }

    @Override
    @Transactional
    public void updateAdRequestBusinessQuestionnaire(UUID adRequestId, BusinessQuestionnaireAnswersRequestDto answers) {
        Client client = authenticatedUserService.validateActiveSubscription().client();
        businessQuestionnaireService.updateQuestionnaireForAdRequest(client.getId(), adRequestId, answers);
    }

    @Override
    @Transactional(readOnly = true)
    public NamedDownloadResource exportAdRequestBusinessQuestionnaireTxtAdmin(UUID adRequestId) {
        authenticatedUserService.validateAdminOrAdsManageAccess();
        byte[] bytes = businessQuestionnaireService.exportTxtForAdRequest(adRequestId, null, true);
        String fileName = businessQuestionnaireService.resolveExportFileNameForAdRequest(adRequestId, null, true);
        return NamedDownloadResource.textPlain(bytes, fileName);
    }

    @Override
    @Transactional
    public void uploadAds(AttachmentRequestDto request, UUID clientId) {
        request.validate();

        Client actor = authenticatedUserService.getLoggedUser().client();
        Client client = clientProfileService.findActiveEntityById(clientId);

        if (actor.isPartner() && actor.getId().equals(clientId)) {
            validateMaxAds(client);
            adApprovalWorkflow.saveAds(request, client);
            return;
        }

        Client admin = authenticatedUserService.validateAdmin().client();

        if (admin.getId().equals(clientId) || Role.ADMIN.equals(client.getRole())) {
            adApprovalWorkflow.saveAds(request, admin);
            return;
        }

        if (client.isPartner()) {
            throw new BusinessRuleException(ClientValidationMessages.PARTNER_USE_AD_REQUEST_UPLOAD);
        }

        if (Objects.isNull(client.getAdRequest())) {
            throw new ResourceNotFoundException(ClientValidationMessages.AD_REQUEST_NOT_FOUND);
        }

        boolean isReplacingExistingAd = client.getAdRequest() != null && client.getAdRequest().getAd() != null;
        if (!isReplacingExistingAd) {
            validateMaxAds(client);
        }
        adApprovalWorkflow.saveAds(request, client);
    }

    @Override
    @Transactional
    public void uploadAdsForAdRequest(AttachmentRequestDto request, UUID adRequestId) {
        request.validate();
        authenticatedUserService.validateAdminOrAdsManageAccess();
        AdRequest adRequest = helper.getAdRequestById(adRequestId);
        if (!AdRequestOrigin.PARTNER.equals(adRequest.getRequestOrigin())) {
            throw new BusinessRuleException(ClientValidationMessages.AD_REQUEST_NOT_PARTNER);
        }
        if (!PartnerSubmissionMode.ADMIN_MATERIALS.equals(adRequest.getSubmissionMode())) {
            throw new BusinessRuleException(ClientValidationMessages.AD_REQUEST_NOT_MATERIALS);
        }
        Client partner = adRequest.getClient();
        adApprovalWorkflow.saveAdsForAdRequest(request, adRequest);
    }

    @Override
    @Transactional
    public void approveAdRequestToAds(UUID adRequestId) {
        authenticatedUserService.validateAdminOrAdsManageAccess();
        AdRequest adRequest = helper.getAdRequestById(adRequestId);
        Client admin = authenticatedUserService.getLoggedUser().client();
        adValidationService.adminApproveAdRequestToAds(adRequest, admin);
    }

    @Override
    @Transactional
    public void cancelAdRequest(UUID adRequestId) {
        authenticatedUserService.validateAdminOrAdsManageAccess();
        AdRequest adRequest = helper.getAdRequestById(adRequestId);
        adValidationService.cancelAdRequest(adRequest);
    }

    @Override
    @Transactional
    public void validateAd(UUID adId, AdValidationType validation, RefusedAdRequestDto request) {
        Ad ad = helper.getAdById(adId);
        Client actor = authenticatedUserService.getLoggedUser().client();
        adValidationService.validateAd(ad, actor, validation, request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdMessageResponseDto> listAdMessages(UUID adId) {
        Client actor = authenticatedUserService.getLoggedUser().client();
        Ad ad = helper.getAdById(adId);
        validateCanSeeAdConversation(actor, ad);
        return adMessageRepository.findAllByAdIdOrderByCreatedAtAsc(adId).stream()
                .map(AdMessageResponseDto::new)
                .toList();
    }

    @Override
    @Transactional
    public void sendAdMessage(UUID adId, AdMessageRequestDto request) {
        Client actor = authenticatedUserService.getLoggedUser().client();
        Ad ad = helper.getAdById(adId);
        validateCanSeeAdConversation(actor, ad);

        AdMessage msg = new AdMessage(ad, actor.getRole(), request.getMessage());
        msg.setUsernameCreate(actor.getBusinessName());
        adMessageRepository.save(msg);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminClientMessageRowDto> listClientMessagesHistory(UUID clientId) {
        authenticatedUserService.validateAdmin();
        return adMessageRepository.findAllByClientIdOrderByCreatedAtAsc(clientId).stream()
                .map(AdminClientMessageRowDto::new)
                .toList();
    }

    private void validateCanSeeAdConversation(Client actor, Ad ad) {
        if (actor.isAdmin() || actor.isDeveloper()) {
            return;
        }
        if (ad.getClient() != null && actor.getId().equals(ad.getClient().getId())) {
            return;
        }
        throw new ForbiddenException("You are not allowed to access this ad conversation.");
    }

    private void validateMaxAds(Client client) {
        boolean hasReachedMaxAds = client.getAds().size() >= SharedConstants.MAX_ADS_PER_CLIENT;

        if (hasReachedMaxAds) {
            throw new BusinessRuleException(ClientValidationMessages.MAX_ADS_REACHED);
        }
    }
}
