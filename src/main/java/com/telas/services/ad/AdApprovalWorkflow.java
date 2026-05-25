package com.telas.services.ad;

import com.telas.dtos.request.AttachmentRequestDto;
import com.telas.dtos.request.RefusedAdRequestDto;
import com.telas.entities.Ad;
import com.telas.entities.AdRequest;
import com.telas.entities.Attachment;
import com.telas.entities.Client;
import com.telas.enums.AdRequestOrigin;
import com.telas.enums.AdValidationType;
import com.telas.enums.NotificationReference;
import com.telas.helpers.MonitorHelper;
import com.telas.helpers.SubscriptionHelper;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.AdRepository;
import com.telas.repositories.AdRequestRepository;
import com.telas.repositories.ClientRepository;
import com.telas.services.NotificationService;
import com.telas.services.notification.AdminAdsNotificationService;
import com.telas.shared.audit.CustomRevisionListener;
import com.telas.shared.constants.valitation.AdValidationMessages;
import com.telas.shared.constants.valitation.ClientValidationMessages;
import com.telas.shared.utils.ClientPortalLinkResolver;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdApprovalWorkflow {

    private final AdUploadService adUploadService;
    private final AdRepository adRepository;
    private final AdRequestRepository adRequestRepository;
    private final ClientRepository clientRepository;
    private final SubscriptionHelper subscriptionHelper;
    private final MonitorHelper monitorHelper;
    private final NotificationService notificationService;
    private final AdminAdsNotificationService adminAdsNotificationService;
    private final ClientPortalLinkResolver clientPortalLinkResolver;
    private final AdValidationService adValidationService;

    @Transactional
    public void saveAds(AttachmentRequestDto request, Client client) {
        if (client.isPartner()) {
            throw new BusinessRuleException(ClientValidationMessages.PARTNER_USE_AD_REQUEST_UPLOAD);
        }
        if (client.getAdRequest() == null) {
            throw new ResourceNotFoundException(AdValidationMessages.AD_REQUEST_NOT_FOUND);
        }
        Ad ad = client.isPrivilegedPanelUser()
                ? (request.getId() == null ? createNewAd(request, client) : updateExistingAd(request))
                : (client.getAdRequest().getAd() == null
                ? createNewAdFromRequest(client.getAdRequest(), request)
                : updateExistingAdFromRequest(client.getAdRequest().getAd(), request, client));
        adUploadService.uploadMedia(request, ad);
        clientRepository.save(client);
    }

    @Transactional
    public void saveAdsForAdRequest(AttachmentRequestDto request, AdRequest adRequest) {
        Client client = adRequest.getClient();
        Ad ad = adRequest.getAd() == null
                ? createNewAdFromRequest(adRequest, request)
                : updateExistingAdFromRequest(adRequest.getAd(), request, client);
        adUploadService.uploadMedia(request, ad);
        clientRepository.save(client);
    }

    @Transactional
    public void adminDeliverPartnerCreativeForReview(Ad ad, AttachmentRequestDto request, Client admin) {
        request.validate();
        if (ad.getClient() == null || !ad.getClient().isPartner()) {
            throw new BusinessRuleException(AdValidationMessages.AD_NOT_PARTNER_ADVERTISER);
        }
        AdValidationType validation = ad.getValidation();
        if (!AdValidationType.APPROVED.equals(validation) && !AdValidationType.REJECTED.equals(validation)) {
            throw new BusinessRuleException(AdValidationMessages.AD_MUST_BE_APPROVED_FOR_PARTNER_REVIEW_DELIVERY);
        }

        removePartnerMaterialsAdFromScreens(ad);

        CustomRevisionListener.setUsername(admin.getBusinessName());
        adUploadService.replaceAdMedia(request, ad);
        ad.setValidation(AdValidationType.PENDING);
        ad.setOnAirNotifiedAt(null);
        ad.setPartnerBoxStagedAt(null);
        adUploadService.uploadMedia(request, ad);
        adRepository.save(ad);

        if (ad.getAdRequest() != null) {
            ad.getAdRequest().openRequest();
            adRequestRepository.save(ad.getAdRequest());
        }

        Client partner = ad.getClient();
        notificationService.save(
                NotificationReference.AD_RECEIVED,
                partner,
                Map.of(
                        "name", partner.getBusinessName(),
                        "link", clientAdsReviewLink(partner)
                ),
                true
        );
    }

    @Transactional
    public void adminApproveAdRequestToAds(AdRequest adRequest, Client admin) {
        adValidationService.adminApproveAdRequestToAds(adRequest, admin);
    }

    @Transactional
    public void cancelAdRequest(AdRequest adRequest) {
        adValidationService.cancelAdRequest(adRequest);
    }

    @Transactional
    public void validateAd(Ad entity, Client actor, AdValidationType validation, RefusedAdRequestDto request) {
        adValidationService.validateAd(entity, actor, validation, request);
    }

    public void notifyAdminsClientFirstAttachmentsUploaded(Client client) {
        if (client == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put("clientName", client.getBusinessName());
        params.put("link", clientPortalLinkResolver.adminClientLink(client.getId()));
        adminAdsNotificationService.notifyAdmins(NotificationReference.ADMIN_CLIENT_FIRST_ATTACHMENTS_UPLOADED, params);
    }

    private Ad createNewAd(AttachmentRequestDto request, Client client) {
        Ad ad = new Ad(request, client);
        setAdValidationDuringUpdate(ad);
        Ad savedAd = adRepository.save(ad);
        client.getAds().add(savedAd);
        return savedAd;
    }

    private Ad updateExistingAd(AttachmentRequestDto request) {
        Ad ad = adUploadService.findAdById(request.getId());
        return adRepository.save(updateAdDetails(request, ad));
    }

    private Ad createNewAdFromRequest(AdRequest entity, AttachmentRequestDto request) {
        Client client = entity.getClient();
        Ad newAd = adUploadService.createAdEntityFromRequest(entity, request);

        if (!ValidateDataUtils.isNullOrEmptyString(entity.getAttachmentIds())) {
            List<Attachment> attachments = adUploadService.getAttachmentsFromAdRequest(entity);
            adRepository.save(newAd);
            newAd.getAttachments().addAll(attachments);
        }

        client.getAds().add(newAd);
        entity.closeRequest();

        adRepository.save(newAd);
        adRequestRepository.save(entity);
        adUploadService.markReferenceAttachmentsConsumed(entity);

        if (AdRequestOrigin.PARTNER.equals(entity.getRequestOrigin())) {
            newAd.setValidation(AdValidationType.PENDING);
            adRepository.save(newAd);
        }

        String recipientLink = clientAdsReviewLink(client);
        notificationService.save(
                NotificationReference.AD_RECEIVED,
                client,
                Map.of(
                        "name", client.getBusinessName(),
                        "link", recipientLink
                ),
                true
        );

        return newAd;
    }

    private Ad updateExistingAdFromRequest(Ad ad, AttachmentRequestDto adRequest, Client actor) {
        boolean resubmitAfterClientRejection = AdValidationType.REJECTED.equals(ad.getValidation());

        updateAdDetails(adRequest, ad);

        if (shouldRemoveAdFromMonitors(ad)) {
            monitorHelper.sendBoxesMonitorsRemoveAd(ad, Collections.singletonList(ad.getName()));
        }

        ad.getAdRequest().closeRequest();
        adRequestRepository.save(ad.getAdRequest());
        adUploadService.markReferenceAttachmentsConsumed(ad.getAdRequest());

        if (!ad.canBeRefused()) {
            ad.getRefusedAds().remove(0);
        }

        adRepository.save(ad);

        if (resubmitAfterClientRejection) {
            Map<String, String> clientParams = new HashMap<>();
            clientParams.put("link", clientAdsReviewLink(ad.getClient()));
            clientParams.put("adName", ad.getName());
            clientParams.put("name", ad.getClient().getBusinessName());
            notificationService.save(
                    NotificationReference.AD_RESUBMITTED_FOR_VALIDATION,
                    ad.getClient(),
                    clientParams,
                    true
            );
            notifyAdminsAdResubmittedToClient(ad, actor);
        } else {
            notificationService.save(
                    NotificationReference.AD_RECEIVED,
                    ad.getClient(),
                    Map.of(
                            "name", ad.getClient().getBusinessName(),
                            "link", clientAdsReviewLink(ad.getClient())
                    ),
                    true
            );
        }

        return ad;
    }

    private void notifyAdminsAdResubmittedToClient(Ad ad, Client actingAdmin) {
        Map<String, String> params = new HashMap<>();
        params.put("clientName", ad.getClient().getBusinessName());
        params.put("adName", ad.getName());
        params.put("adminName", actingAdmin != null ? actingAdmin.getBusinessName() : "");
        params.put("link", clientPortalLinkResolver.adminAdsLink());
        adminAdsNotificationService.notifyAdmins(NotificationReference.ADMIN_AD_RESUBMITTED_TO_CLIENT, params);
    }

    private Ad updateAdDetails(AttachmentRequestDto request, Ad ad) {
        if (!AdValidationType.REJECTED.equals(ad.getValidation())) {
            adUploadService.verifyFileNameChanged(request, ad);
        }
        adUploadService.replaceAdMedia(request, ad);
        setAdValidationDuringUpdate(ad);
        return ad;
    }

    private void setAdValidationDuringUpdate(Ad entity) {
        Client client = entity.getClient();

        if (client.isPrivilegedPanelUser()) {
            entity.setValidation(AdValidationType.APPROVED);
            return;
        }
        if (client.isPartner() && entity.getAdRequest() == null) {
            entity.setValidation(AdValidationType.APPROVED);
            return;
        }
        if (client.isPartner() && entity.getAdRequest() != null) {
            entity.setValidation(AdValidationType.PENDING);
            return;
        }
        entity.setValidation(AdValidationType.PENDING);
    }

    private boolean shouldRemoveAdFromMonitors(Ad ad) {
        Client client = ad.getClient();
        return AdValidationType.APPROVED.equals(ad.getValidation())
                && !client.isPrivilegedPanelUser()
                && !subscriptionHelper.getClientActiveSubscriptions(client.getId()).isEmpty();
    }

    private void removePartnerMaterialsAdFromScreens(Ad ad) {
        if (ad == null) {
            return;
        }
        ad.setOnAirNotifiedAt(null);
        ad.setPartnerBoxStagedAt(null);
        adRepository.save(ad);
        if (!ValidateDataUtils.isNullOrEmptyString(ad.getName())) {
            monitorHelper.sendBoxesMonitorsRemoveAd(ad, List.of(ad.getName()));
        }
    }

    private String clientAdsReviewLink(Client client) {
        return clientPortalLinkResolver.clientPartnerAdsReviewLink(client);
    }
}
