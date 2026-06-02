package com.telas.services.ad;

import com.telas.dtos.request.RefusedAdRequestDto;
import com.telas.dtos.request.UpdateBoxMonitorsAdRequestDto;
import com.telas.entities.*;
import com.telas.enums.AdRequestOrigin;
import com.telas.enums.AdValidationType;
import com.telas.enums.NotificationReference;
import com.telas.enums.PartnerSubmissionMode;
import com.telas.helpers.AdOnAirNotificationHelper;
import com.telas.helpers.MonitorHelper;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ForbiddenException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.AdRepository;
import com.telas.repositories.AdRequestRepository;
import com.telas.repositories.MonitorAdRepository;
import com.telas.repositories.MonitorRepository;
import com.telas.services.AdUnusedTrackingService;
import com.telas.services.NotificationService;
import com.telas.services.PartnerSlotAccessService;
import com.telas.services.notification.AdminAdsNotificationService;
import com.telas.services.partner.PartnerPlacementRules;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.audit.CustomRevisionListener;
import com.telas.shared.constants.valitation.AdValidationMessages;
import com.telas.shared.constants.valitation.AttachmentValidationMessages;
import com.telas.shared.constants.valitation.ClientValidationMessages;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.shared.utils.ClientPortalLinkResolver;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AdValidationService {

    private final AdRepository adRepository;
    private final AdRequestRepository adRequestRepository;
    private final MonitorAdRepository monitorAdRepository;
    private final MonitorRepository monitorRepository;
    private final NotificationService notificationService;
    private final AdminAdsNotificationService adminAdsNotificationService;
    private final ClientPortalLinkResolver clientPortalLinkResolver;
    private final AdUnusedTrackingService adUnusedTrackingService;
    private final MonitorHelper monitorHelper;
    private final AdOnAirNotificationHelper adOnAirNotificationHelper;
    private final PartnerPlacementRules partnerPlacementRules;
    private final PartnerSlotAccessService partnerSlotAccessService;

    @Transactional
    public void adminApproveAdRequestToAds(AdRequest adRequest, Client admin) {
        if (!PartnerSubmissionMode.PARTNER_FINISHED_CREATIVE.equals(adRequest.getSubmissionMode())) {
            throw new BusinessRuleException(ClientValidationMessages.AD_REQUEST_NOT_FINISHED_CREATIVE);
        }
        Ad ad = adRequest.getAd();
        if (ad == null) {
            throw new ResourceNotFoundException(AdValidationMessages.AD_NOT_FOUND);
        }
        if (!AdValidationType.PENDING.equals(ad.getValidation())) {
            throw new BusinessRuleException(AdValidationMessages.AD_ALREADY_VALIDATED);
        }
        CustomRevisionListener.setUsername(admin.getBusinessName());
        ad.setValidation(AdValidationType.APPROVED);
        adRepository.save(ad);
        adUnusedTrackingService.syncUnusedStateForAdIds(List.of(ad.getId()));
        adRequest.closeRequest();
        adRequestRepository.save(adRequest);
        Ad refreshed = adRepository.findByIdWithClientAndAdRequest(ad.getId()).orElse(ad);
        notifyClientApprovedAd(refreshed);
        notifyAdminsClientApprovedAdForAdsTab(refreshed);
    }

    @Transactional
    public void cancelAdRequest(AdRequest adRequest) {
        Ad ad = adRequest.getAd();
        if (ad != null && AdValidationType.APPROVED.equals(ad.getValidation())) {
            throw new BusinessRuleException(ClientValidationMessages.AD_REQUEST_CANNOT_CANCEL);
        }
        if (ad != null && isPartnerMaterialsFlowAd(ad)) {
            removePartnerMaterialsAdFromScreens(ad);
        }
        adRequest.closeRequest();
        adRequestRepository.save(adRequest);
    }

    @Transactional
    public void validateAd(Ad entity, Client actor, AdValidationType validation, RefusedAdRequestDto request) {
        if (AdValidationType.PENDING.equals(validation)) {
            throw new BusinessRuleException(AdValidationMessages.PENDING_VALIDATION_NOT_ACCEPTED);
        }

        if (AdValidationType.APPROVED.equals(entity.getValidation())) {
            return;
        }

        validateValidatorPermissions(entity, actor);

        if (!entity.canBeRefused() && AdValidationType.REJECTED.equals(validation)) {
            throw new BusinessRuleException(AdValidationMessages.AD_EXCEEDS_MAX_VALIDATION);
        }

        CustomRevisionListener.setUsername(entity.getClient().getBusinessName());
        entity.setValidation(validation);

        if (AdValidationType.REJECTED.equals(validation)) {
            validateRejectionRequest(request);
            createRefusedAd(request, entity);
        }

        adRepository.save(entity);
        adUnusedTrackingService.syncUnusedStateForAdIds(List.of(entity.getId()));

        if (AdValidationType.REJECTED.equals(validation)) {
            if (isPartnerMaterialsFlowAd(entity)) {
                removePartnerMaterialsAdFromScreens(entity);
            }
            notifyAdminsClientRejectedAd(entity, request);
        } else if (AdValidationType.APPROVED.equals(validation)) {
            if (isPartnerMaterialsFlowAd(entity)) {
                removePartnerMaterialsAdFromScreens(entity);
                Ad refreshed = adRepository.findByIdWithClientAndAdRequest(entity.getId()).orElse(entity);
                notifyClientApprovedAd(refreshed);
                notifyAdminsClientApprovedAd(refreshed);
            } else {
                if (entity.getAdRequest() != null) {
                    attachAdToTargetMonitorIfNeeded(entity, entity.getAdRequest());
                }
                syncMonitorsPlaylistAfterAdApproved(entity);
                notifyPartnerOnAirAfterApprovalIfEligible(entity);
                Ad refreshed = adRepository.findByIdWithClientAndAdRequest(entity.getId()).orElse(entity);
                notifyClientApprovedAd(refreshed);
                notifyAdminsClientApprovedAd(refreshed);
            }
        }
    }

    private void attachAdToTargetMonitorIfNeeded(Ad ad, AdRequest adRequest) {
        if (adRequest.getTargetMonitor() == null) {
            return;
        }
        if (monitorAdRepository.countByAdId(ad.getId()) > 0) {
            return;
        }
        Monitor monitor = monitorRepository.findById(adRequest.getTargetMonitor().getId())
                .orElseThrow(() -> new ResourceNotFoundException(MonitorValidationMessages.MONITOR_NOT_FOUND));
        Client advertiser = ad.getClient();
        if (advertiser != null && advertiser.isPartner()) {
            partnerSlotAccessService.assertCanAddBlocks(
                    advertiser,
                    monitor,
                    SharedConstants.MIN_QUANTITY_MONITOR_BLOCK);
        }
        monitor.getMonitorAds().add(new MonitorAd(monitor, ad));
        monitorRepository.save(monitor);
    }

    private void syncMonitorsPlaylistAfterAdApproved(Ad ad) {
        List<MonitorAd> placements = monitorAdRepository.findByAdIdWithMonitor(ad.getId());
        if (placements == null || placements.isEmpty()) {
            return;
        }
        Map<UUID, Monitor> monitorsById = new LinkedHashMap<>();
        for (MonitorAd placement : placements) {
            Monitor monitor = placement.getMonitor();
            if (monitor != null) {
                monitorsById.putIfAbsent(monitor.getId(), monitor);
            }
        }
        Client advertiser = ad.getClient();
        for (Monitor monitor : monitorsById.values()) {
            if (!monitor.isAbleToSendBoxRequest()) {
                continue;
            }
            if (advertiser != null
                    && advertiser.isPartner()
                    && partnerPlacementRules.isForeignPlacementForPartner(advertiser, monitor)) {
                continue;
            }
            List<UpdateBoxMonitorsAdRequestDto> playlist = monitorHelper.buildOrderedBoxUpdateDtos(monitor);
            monitorHelper.syncBoxAdsPlaylist(monitor, playlist);
        }
    }

    private void notifyPartnerOnAirAfterApprovalIfEligible(Ad ad) {
        Client partner = ad.getClient();
        if (partner == null || !partner.isPartner()) {
            return;
        }
        List<MonitorAd> placements = monitorAdRepository.findByAdIdWithMonitor(ad.getId());
        if (placements == null || placements.isEmpty()) {
            return;
        }
        Map<UUID, Monitor> monitorsById = new LinkedHashMap<>();
        for (MonitorAd placement : placements) {
            Monitor monitor = placement.getMonitor();
            if (monitor != null && !partnerPlacementRules.isForeignPlacementForPartner(partner, monitor)) {
                monitorsById.putIfAbsent(monitor.getId(), monitor);
            }
        }
        for (Monitor monitor : monitorsById.values()) {
            if (!monitor.isAbleToSendBoxRequest()) {
                continue;
            }
            List<MonitorAd> adsOnMonitor = placements.stream()
                    .filter(ma -> ma.getMonitor() != null && monitor.getId().equals(ma.getMonitor().getId()))
                    .toList();
            adOnAirNotificationHelper.notifyOnAirForNewMonitorAds(adsOnMonitor, monitor, true, false);
        }
    }

    private boolean isPartnerMaterialsFlowAd(Ad ad) {
        if (ad == null || ad.getClient() == null || !ad.getClient().isPartner() || ad.getAdRequest() == null) {
            return false;
        }
        return AdRequestOrigin.PARTNER.equals(ad.getAdRequest().getRequestOrigin());
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

    private void notifyClientApprovedAd(Ad entity) {
        Client client = entity.getClient();
        boolean partner = client != null && client.isPartner();
        boolean liveOnScreen = partner && isPartnerAdLiveOnScreen(entity, client);
        Map<String, String> params = new HashMap<>();
        params.put("name", client.getBusinessName());
        params.put("adName", entity.getName());
        params.put("link", clientPortalLinkResolver.clientApprovedConfirmationLink(client, liveOnScreen));
        params.put("partner", clientPortalLinkResolver.partnerFlag(client));
        params.put("liveOnScreen", liveOnScreen ? "true" : "false");
        params.put("linkLabel", liveOnScreen ? "My screens" : (partner ? "Review ads" : "My Telas — Ads"));
        notificationService.save(NotificationReference.CLIENT_AD_APPROVED_CONFIRMATION, client, params, true);
    }

    private boolean isPartnerAdLiveOnScreen(Ad ad, Client partner) {
        if (ad.getOnAirNotifiedAt() != null) {
            return true;
        }
        List<MonitorAd> placements = monitorAdRepository.findByAdIdWithMonitor(ad.getId());
        if (placements == null || placements.isEmpty()) {
            return false;
        }
        for (MonitorAd placement : placements) {
            Monitor monitor = placement.getMonitor();
            if (monitor == null) {
                continue;
            }
            if (monitor.isAbleToSendBoxRequest()) {
                return true;
            }
        }
        return false;
    }

    private void notifyAdminsClientApprovedAd(Ad entity) {
        Client client = entity.getClient();
        Map<String, String> params = buildAdminAdActorParams(client, entity);
        params.put("link", clientPortalLinkResolver.adminClientMessagesLink(client.getId()));
        adminAdsNotificationService.notifyAdmins(NotificationReference.ADMIN_CLIENT_AD_APPROVED, params);
    }

    private void notifyAdminsClientApprovedAdForAdsTab(Ad entity) {
        Client client = entity.getClient();
        Map<String, String> params = buildAdminAdActorParams(client, entity);
        params.put("link", clientPortalLinkResolver.adminAdsLink());
        adminAdsNotificationService.notifyAdmins(NotificationReference.ADMIN_CLIENT_AD_APPROVED, params);
    }

    private void notifyAdminsClientRejectedAd(Ad entity, RefusedAdRequestDto request) {
        Client client = entity.getClient();
        Map<String, String> params = new HashMap<>();
        params.put("name", client.getBusinessName());
        params.put("adName", entity.getName());
        params.put("actorType", client.isPartner() ? "partner" : "customer");
        params.put("locations", "");
        if (request != null) {
            if (!ValidateDataUtils.isNullOrEmptyString(request.getJustification())) {
                params.put("justification", request.getJustification());
            }
            if (!ValidateDataUtils.isNullOrEmptyString(request.getDescription())) {
                params.put("description", request.getDescription());
            }
        }
        Map<String, String> adminParams = new HashMap<>(params);
        adminParams.put("link", clientPortalLinkResolver.adminClientMessagesLink(client.getId()));
        adminAdsNotificationService.notifyAdmins(NotificationReference.CLIENT_AD_REJECTED, adminParams);

        Map<String, String> clientParams = new HashMap<>(params);
        clientParams.put("link", clientPortalLinkResolver.clientPartnerAdsReviewLink(client));
        notificationService.save(NotificationReference.CLIENT_AD_REJECTION_CONFIRMED, client, clientParams, true);
    }

    private Map<String, String> buildAdminAdActorParams(Client client, Ad entity) {
        Map<String, String> params = new HashMap<>();
        params.put("clientName", client.getBusinessName());
        params.put("adName", entity.getName());
        params.put("actorType", client.isPartner() ? "partner" : "customer");
        return params;
    }

    private void validateValidatorPermissions(Ad entity, Client validator) {
        if (!AdValidationType.PENDING.equals(entity.getValidation())) {
            throw new BusinessRuleException(AdValidationMessages.AD_ALREADY_VALIDATED);
        }

        boolean isOwner = validator.getId().equals(entity.getClient().getId());
        Client owner = entity.getClient();

        if (owner != null && owner.isPartner()) {
            if (!isOwner) {
                throw new ForbiddenException(AdValidationMessages.VALIDATION_NOT_ALLOWED);
            }
            return;
        }

        boolean isPanel = validator.isAdmin() || validator.isDeveloper();
        if (!isOwner && !isPanel) {
            throw new ForbiddenException(AdValidationMessages.VALIDATION_NOT_ALLOWED);
        }
    }

    private void validateRejectionRequest(RefusedAdRequestDto request) {
        if (request == null || ValidateDataUtils.isNullOrEmptyString(request.getJustification())) {
            throw new BusinessRuleException(AttachmentValidationMessages.JUSTIFICATION_REQUIRED);
        }
    }

    private void createRefusedAd(RefusedAdRequestDto request, Ad entity) {
        RefusedAd refusedAd = new RefusedAd(request, entity);
        entity.setUsernameUpdate(entity.getClient().getBusinessName());
        entity.getRefusedAds().add(refusedAd);
        if (entity.getAdRequest() != null) {
            entity.getAdRequest().handleRefusal();
            adRequestRepository.save(entity.getAdRequest());
        }
    }
}
