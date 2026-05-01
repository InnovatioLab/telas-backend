package com.telas.helpers;

import com.telas.dtos.request.AttachmentRequestDto;
import com.telas.dtos.request.RefusedAdRequestDto;
import com.telas.dtos.response.LinkResponseDto;
import com.telas.entities.*;
import com.telas.enums.AdValidationType;
import com.telas.enums.NotificationReference;
import com.telas.enums.Permission;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ForbiddenException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.AdRepository;
import com.telas.repositories.AdRequestRepository;
import com.telas.repositories.AttachmentRepository;
import com.telas.repositories.ClientRepository;
import com.telas.services.AdUnusedTrackingService;
import com.telas.services.BucketService;
import com.telas.services.AdminEmailAlertPreferenceService;
import com.telas.services.NotificationService;
import com.telas.services.PermissionService;
import com.telas.shared.audit.CustomRevisionListener;
import com.telas.shared.constants.valitation.AdValidationMessages;
import com.telas.shared.constants.valitation.AttachmentValidationMessages;
import com.telas.shared.utils.AttachmentUtils;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.*;

@Component
@RequiredArgsConstructor
public class AttachmentHelper {
    private final AttachmentRepository attachmentRepository;
    private final AdRepository adRepository;
    private final BucketService bucketService;
    private final NotificationService notificationService;
    private final AdRequestRepository adRequestRepository;
    private final ClientRepository clientRepository;
    private final SubscriptionHelper subscriptionHelper;
    private final MonitorHelper monitorHelper;
    private final PermissionService permissionService;
    private final AdminEmailAlertPreferenceService adminEmailAlertPreferenceService;

    private final AdUnusedTrackingService adUnusedTrackingService;

    @Value("${front.base.url}")
    private String frontBaseUrl;

    @Transactional
    public <T extends AttachmentRequestDto> void validate(List<T> requestList) {
        if (ValidateDataUtils.isNullOrEmpty(requestList)) {
            throw new BusinessRuleException(AttachmentValidationMessages.ATTACHMENT_LIST_EMPTY);
        }

        requestList.forEach(AttachmentRequestDto::validate);
    }

    List<Attachment> getAttachmentsByIds(List<UUID> attachmentsIds) {
        return attachmentRepository.findByIdIn(attachmentsIds).orElseThrow(() -> new BusinessRuleException(AttachmentValidationMessages.ATTACHMENT_NOT_FOUND));
    }


    private List<LinkResponseDto> getAttachmentsLinksResponseFromAdRequest(AdRequest adRequestEntity) {
        if (ValidateDataUtils.isNullOrEmptyString(adRequestEntity.getAttachmentIds())) {
            return Collections.emptyList();
        }

        List<Attachment> attachments = getAttachmentsFromAdRequest(adRequestEntity);
        return attachments.stream()
                .map(attachment -> new LinkResponseDto(
                        attachment.getId(),
                        attachment.getName(),
                        bucketService.getLink(AttachmentUtils.format(attachment)),
                        bucketService.getDownloadLink(AttachmentUtils.format(attachment), attachment.getName())))
                .toList();
    }


    private LinkResponseDto getAdLinkResponseFromAdRequest(AdRequest adRequestEntity) {
        if (adRequestEntity.getAd() == null) {
            return null;
        }
        Ad ad = adRequestEntity.getAd();
        return new LinkResponseDto(
                ad.getId(),
                ad.getName(),
                bucketService.getLink(AttachmentUtils.format(ad)),
                bucketService.getDownloadLink(AttachmentUtils.format(ad), ad.getName()));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAdRequestData(AdRequest adRequest) {
        Map<String, Object> response = new HashMap<>();
        response.put("ad", getAdLinkResponseFromAdRequest(adRequest));
        response.put("attachments", getAttachmentsLinksResponseFromAdRequest(adRequest));
        return response;
    }

    @Transactional(readOnly = true)
    public String getStringLinkFromAd(Ad adEntity) {
        return bucketService.getLink(AttachmentUtils.format(adEntity));
    }

    @Transactional(readOnly = true)
    public String getDownloadLinkFromAd(Ad adEntity) {
        return bucketService.getDownloadLink(AttachmentUtils.format(adEntity), adEntity.getName());
    }


    @Transactional
    public List<Attachment> getAttachmentsFromAdRequest(AdRequest adRequestEntity) {
        List<UUID> attachmentIds = Arrays.stream(adRequestEntity.getAttachmentIds().split(","))
                .map(UUID::fromString)
                .toList();

        return getAttachmentsByIds(attachmentIds);
    }

    @Transactional
    public void saveAttachments(List<AttachmentRequestDto> requestList, Client client) {
        requestList.forEach(request -> {
            Attachment attachment = (request.getId() == null)
                    ? createNewAttachment(request, client)
                    : updateExistingAttachment(request);
            client.getAttachments().add(attachment);
            uploadAttachment(request, attachment);
        });
        clientRepository.save(client);
    }

    private Attachment createNewAttachment(AttachmentRequestDto request, Client client) {
        Attachment newAttachment = new Attachment(request, client);
        newAttachment.setUsernameCreate(client.getBusinessName());
        return attachmentRepository.save(newAttachment);
    }

    private Attachment updateExistingAttachment(AttachmentRequestDto request) {
        Attachment entity = attachmentRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException(AttachmentValidationMessages.ATTACHMENT_NOT_FOUND));
        bucketService.deleteAttachment(AttachmentUtils.format(entity));
        entity.setName(request.getName());
        entity.setType(request.getType());
        return attachmentRepository.save(entity);
    }

    private <T extends AttachmentRequestDto> void uploadAttachment(T attachment, Object entity) {
        if (entity instanceof Ad || entity instanceof Attachment) {
            bucketService.upload(
                    attachment.getBytes(),
                    AttachmentUtils.format(entity),
                    attachment.getType(),
                    new ByteArrayInputStream(attachment.getBytes())
            );
        }
    }

    private void verifyFileNameChanged(AttachmentRequestDto request, Object entity) {
        String entityName = entity instanceof Attachment ? ((Attachment) entity).getName() : ((Ad) entity).getName();

        if (entityName.equals(request.getName())) {
            throw new BusinessRuleException(AdValidationMessages.FILE_NAME_MUST_BE_CHANGED_DURING_UPDATE);
        }
    }

    @Transactional
    public void saveAds(AttachmentRequestDto request, Client client) {
        Ad ad = client.isPrivilegedPanelUser() || (client.isPartner() && !client.getApprovedAds().isEmpty())
                ? (request.getId() == null ? createNewAd(request, client) : updateExistingAd(request))
                : (client.getAdRequest().getAd() == null ? createNewAdFromRequest(client.getAdRequest(), request) : updateExistingAdFromRequest(client.getAdRequest().getAd(), request, client));
        uploadAttachment(request, ad);
        clientRepository.save(client);
    }

    private Ad createNewAd(AttachmentRequestDto request, Client client) {
        Ad ad = new Ad(request, client);
        setAdValidationDuringUpdate(ad);
        Ad savedAd = adRepository.save(ad);
        client.getAds().add(savedAd);
        return savedAd;
    }

    private Ad updateExistingAd(AttachmentRequestDto request) {
        Ad ad = findAdById(request.getId());
        return adRepository.save(updateAdDetails(request, ad));
    }

    private Ad createNewAdFromRequest(AdRequest entity, AttachmentRequestDto request) {
        Client client = entity.getClient();
        Ad newAd = new Ad(request, client, entity);

        if (!ValidateDataUtils.isNullOrEmptyString(entity.getAttachmentIds())) {
            List<Attachment> attachments = getAttachmentsFromAdRequest(entity);
            adRepository.save(newAd);
            newAd.getAttachments().addAll(attachments);
        }

        client.getAds().add(newAd);
        entity.closeRequest();

        adRepository.save(newAd);
        adRequestRepository.save(entity);

        notificationService.save(
                NotificationReference.AD_RECEIVED,
                client,
                Map.of("link", frontBaseUrl + "/client/my-telas?tab=ads"),
                false
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

        if (!ad.canBeRefused()) {
            ad.getRefusedAds().remove(0);
        }

        adRepository.save(ad);

        if (resubmitAfterClientRejection) {
            Map<String, String> clientParams = new HashMap<>();
            clientParams.put("link", frontBaseUrl + "/client/my-telas?tab=ads");
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
                    Map.of("link", frontBaseUrl + "/client/my-telas?tab=ads"),
                    false
            );
        }

        return ad;
    }

    private void notifyAdminsAdResubmittedToClient(Ad ad, Client actingAdmin) {
        String adminLink = frontBaseUrl + "/admin/ads";
        Map<String, String> params = new HashMap<>();
        params.put("clientName", ad.getClient().getBusinessName());
        params.put("adName", ad.getName());
        params.put("adminName", actingAdmin != null ? actingAdmin.getBusinessName() : "");
        params.put("link", adminLink);
        for (Client recipient : clientRepository.findAllAdminsAndDevelopers()) {
            boolean canManageAds = recipient.isDeveloper()
                    || permissionService.hasPermission(recipient, Permission.ADMIN_ADS_MANAGE);
            if (!canManageAds) {
                continue;
            }
            boolean sendEmail = adminEmailAlertPreferenceService.wantsEmail(
                    recipient.getId(), com.telas.enums.AdminEmailAlertCategory.ADS_MANAGEMENT);
            notificationService.save(
                    NotificationReference.ADMIN_AD_RESUBMITTED_TO_CLIENT,
                    recipient,
                    new HashMap<>(params),
                    sendEmail
            );
        }
    }

    private Ad updateAdDetails(AttachmentRequestDto request, Ad ad) {
        verifyFileNameChanged(request, ad);
        bucketService.deleteAttachment(AttachmentUtils.format(ad));
        ad.setName(request.getName());
        ad.setType(request.getType());
        setAdValidationDuringUpdate(ad);
        return ad;
    }

    private void setAdValidationDuringUpdate(Ad entity) {
        Client client = entity.getClient();

        if (client.isPrivilegedPanelUser()) {
            entity.setValidation(AdValidationType.APPROVED);
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

    private Ad findAdById(UUID adId) {
        return adRepository.findById(adId)
                .orElseThrow(() -> new ResourceNotFoundException(AdValidationMessages.AD_NOT_FOUND));
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
            notifyAdminsClientRejectedAd(entity, request);
        }
    }

    private void notifyAdminsClientRejectedAd(Ad entity, RefusedAdRequestDto request) {
        Client client = entity.getClient();
        String adminLink = frontBaseUrl + "/admin/clients/" + client.getId() + "/messages";
        String clientLink = frontBaseUrl + "/client/my-telas?tab=ads";
        Map<String, String> params = new HashMap<>();
        params.put("name", client.getBusinessName());
        params.put("adName", entity.getName());
        params.put("locations", "");
        if (request != null) {
            if (!ValidateDataUtils.isNullOrEmptyString(request.getJustification())) {
                params.put("justification", request.getJustification());
            }
            if (!ValidateDataUtils.isNullOrEmptyString(request.getDescription())) {
                params.put("description", request.getDescription());
            }
        }
        for (Client recipient : clientRepository.findAllAdminsAndDevelopers()) {
            boolean canManageAds = recipient.isDeveloper()
                    || permissionService.hasPermission(recipient, Permission.ADMIN_ADS_MANAGE);
            if (!canManageAds) {
                continue;
            }
            boolean sendEmail = adminEmailAlertPreferenceService.wantsEmail(
                    recipient.getId(), com.telas.enums.AdminEmailAlertCategory.ADS_MANAGEMENT);
            params.put("link", adminLink);
            notificationService.save(NotificationReference.CLIENT_AD_REJECTED, recipient, params, sendEmail);
        }

        params.put("link", clientLink);
        notificationService.save(NotificationReference.CLIENT_AD_REJECTED, client, params, true);
    }

    private void validateValidatorPermissions(Ad entity, Client validator) {
        if (!AdValidationType.PENDING.equals(entity.getValidation())) {
            throw new BusinessRuleException(AdValidationMessages.AD_ALREADY_VALIDATED);
        }

        boolean isOwner = validator.getId().equals(entity.getClient().getId());
        boolean isPanel = validator.isAdmin() || validator.isDeveloper();

        if (!isOwner && !isPanel) {
            throw new ForbiddenException(AdValidationMessages.VALIDATION_NOT_ALLOWED);
        }
    }

    private void validateRejectionRequest(RefusedAdRequestDto request) {
        if (request == null) {
            throw new BusinessRuleException(AttachmentValidationMessages.JUSTIFICATION_REQUIRED);
        }
    }

    private void createRefusedAd(RefusedAdRequestDto request, Ad entity) {
        RefusedAd refusedAd = new RefusedAd(request, entity);
        entity.setUsernameUpdate(entity.getClient().getBusinessName());
        entity.getRefusedAds().add(refusedAd);
        entity.getAdRequest().handleRefusal();
        adRequestRepository.save(entity.getAdRequest());

//        if (Objects.nonNull(entity.getAdRequest())) {
//            entity.getAdRequest().handleRefusal();
//            adRequestRepository.save(entity.getAdRequest());
//        }
    }
}
