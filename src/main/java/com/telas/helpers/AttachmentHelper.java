package com.telas.helpers;

import com.telas.dtos.request.AdRequestDto;
import com.telas.dtos.request.AttachmentRequestDto;
import com.telas.dtos.request.RefusedAdRequestDto;
import com.telas.dtos.response.LinkResponseDto;
import com.telas.entities.*;
import com.telas.enums.AdValidationType;
import com.telas.enums.NotificationReference;
import com.telas.enums.Role;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.AdRepository;
import com.telas.repositories.AdRequestRepository;
import com.telas.repositories.AttachmentRepository;
import com.telas.repositories.ClientRepository;
import com.telas.services.BucketService;
import com.telas.services.NotificationService;
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
  private final MonitorHelper monitorHelper;

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
    List<Attachment> attachments = getAttachmentsFromAdRequest(adRequestEntity);
    return attachments.stream()
            .map(attachment -> new LinkResponseDto(attachment.getId(), bucketService.getLink(AttachmentUtils.format(attachment))))
            .toList();
  }


  private LinkResponseDto getAdLinkResponseFromAdRequest(AdRequest adRequestEntity) {
    if (adRequestEntity.getAd() == null) {
      return null;
    }
    Ad ad = adRequestEntity.getAd();
    return new LinkResponseDto(ad.getId(), bucketService.getLink(AttachmentUtils.format(ad)));
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


  @Transactional
  public List<Attachment> getAttachmentsFromAdRequest(AdRequest adRequestEntity) {
    List<UUID> attachmentIds = Arrays.stream(adRequestEntity.getAttachmentIds().split(","))
            .map(UUID::fromString)
            .toList();

    return getAttachmentsByIds(attachmentIds);
  }

  @Transactional
  public <T extends AttachmentRequestDto> void saveAttachments(List<T> requestList, Client client, AdRequest adRequestEntity) {
    for (T attachment : requestList) {
      if (attachment.getId() == null) {
        handleNewAttachment(attachment, client, adRequestEntity);
      } else {
        handleExistingAttachment(attachment);
      }
    }
    clientRepository.save(client);
  }

  private <T extends AttachmentRequestDto> void handleNewAttachment(T attachment, Client client, AdRequest adRequestEntity) {
    if (attachment instanceof AdRequestDto adRequest && adRequestEntity != null) {
      Ad ad = prepareAd(adRequestEntity, adRequest, client);
      adRequestEntity.closeRequest();
      adRequestRepository.save(adRequestEntity);

      Ad savedAd = adRepository.save(ad);
      uploadAttachment(attachment, savedAd);

      notificationService.save(
              NotificationReference.AD_RECEIVED,
              client,
              Map.of("link", frontBaseUrl + "/ads/" + savedAd.getId()),
              false
      );
    } else if (!(attachment instanceof AdRequestDto) && adRequestEntity == null) {
      Attachment newAttachment = new Attachment(attachment, client);
      newAttachment.setUsernameCreate(client.getBusinessName());
      Attachment savedAttachment = attachmentRepository.save(newAttachment);
      client.getAttachments().add(savedAttachment);

      uploadAttachment(attachment, savedAttachment);
    }
  }

  private Ad prepareAd(AdRequest adRequestEntity, AdRequestDto adRequest, Client client) {
    Ad ad = Optional.ofNullable(adRequestEntity.getAd())
            .orElseGet(() -> createNewAdFromRequest(adRequestEntity, adRequest));

    if (adRequestEntity.getAd() != null) {
      updateExistingAdFromRequest(ad, adRequest);
    }

    if (Role.ADMIN.equals(client.getRole())) {
      ad.setValidation(AdValidationType.APPROVED);
    }

    return ad;
  }

  private Ad createNewAdFromRequest(AdRequest adRequestEntity, AdRequestDto adRequest) {
    List<Attachment> attachments = getAttachmentsFromAdRequest(adRequestEntity);
    Client adOwner = adRequestEntity.getClient();

    Ad newAd = new Ad(adRequest, adOwner, adRequestEntity);
    newAd.getAttachments().addAll(attachments);
    adOwner.getAds().add(newAd);

    return newAd;
  }

  private void updateExistingAdFromRequest(Ad ad, AdRequestDto adRequest) {
    verifyFileNameChanged(adRequest, ad);
    bucketService.deleteAttachment(AttachmentUtils.format(ad));
    List<String> oldAdNameList = Collections.singletonList(ad.getName());
    ad.setName(adRequest.getName());
    ad.setType(adRequest.getType());

    if (shouldRemoveAdFromMonitors(ad)) {
      monitorHelper.sendBoxesMonitorsRemoveAd(ad, oldAdNameList);
    }

    ad.setValidation(AdValidationType.PENDING);
  }

  private boolean shouldRemoveAdFromMonitors(Ad ad) {
    Client client = ad.getClient();
    return AdValidationType.APPROVED.equals(ad.getValidation())
           && !Role.ADMIN.equals(client.getRole())
           && !client.getActiveSubscriptions().isEmpty();
  }

  private <T extends AttachmentRequestDto> void handleExistingAttachment(T attachment) {
    if (!(attachment instanceof AdRequestDto)) {
      Attachment entity = attachmentRepository.findById(attachment.getId())
              .orElseThrow(() -> new ResourceNotFoundException(AttachmentValidationMessages.ATTACHMENT_NOT_FOUND));
      verifyFileNameChanged(attachment, entity);
      bucketService.deleteAttachment(AttachmentUtils.format(entity));
      entity.setName(attachment.getName());
      entity.setType(attachment.getType());

      Attachment savedAttachment = attachmentRepository.save(entity);
      uploadAttachment(attachment, savedAttachment);
    }
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
  public void saveAdminAds(AdRequestDto request, Client admin) {
    Ad ad = request.getId() == null ? createNewAd(request, admin) : updateExistingAd(request);
    uploadAttachment(request, ad);
    clientRepository.save(admin);
  }

  private Ad createNewAd(AdRequestDto request, Client admin) {
    Ad ad = new Ad();
    ad.setName(request.getName());
    ad.setType(request.getType());
    ad.setClient(admin);
    ad.setValidation(AdValidationType.APPROVED);

    Ad savedAd = adRepository.save(ad);
    admin.getAds().add(savedAd);
    return savedAd;
  }

  private Ad updateExistingAd(AdRequestDto request) {
    Ad ad = adRepository.findById(request.getId())
            .orElseThrow(() -> new ResourceNotFoundException(AdValidationMessages.AD_NOT_FOUND));
    verifyFileNameChanged(request, ad);

    bucketService.deleteAttachment(AttachmentUtils.format(ad));
    ad.setName(request.getName());
    ad.setType(request.getType());

    return adRepository.save(ad);
  }

  @Transactional
  public void validateAd(Ad entity, AdValidationType validation, RefusedAdRequestDto request, Client client) {
    if (AdValidationType.PENDING.equals(validation)) {
      throw new BusinessRuleException(AdValidationMessages.PENDING_VALIDATION_NOT_ACCEPTED);
    }

    if (AdValidationType.APPROVED.equals(entity.getValidation())) {
      return;
    }

    if (!entity.getAdRequest().canBeRefused() && AdValidationType.REJECTED.equals(validation)) {
      throw new BusinessRuleException(AdValidationMessages.AD_EXCEEDS_MAX_VALIDATION);
    }

    CustomRevisionListener.setUsername(client.getBusinessName());
    entity.setValidation(validation);

    if (AdValidationType.REJECTED.equals(validation)) {
      if (request == null) {
        throw new BusinessRuleException(AttachmentValidationMessages.JUSTIFICATION_REQUIRED);
      }
      createRefusedAd(request, entity, client);
    }

    adRequestRepository.save(entity.getAdRequest());
  }

  private void createRefusedAd(RefusedAdRequestDto request, Ad entity, Client admin) {
    RefusedAd refusedAd = new RefusedAd(request, entity, admin);
    refusedAd.setUsernameCreate(admin.getBusinessName());
    entity.setUsernameUpdate(admin.getBusinessName());

    AdRequest adRequest = entity.getAdRequest();
    adRequest.getRefusedAds().add(refusedAd);
    adRequest.incrementRefusalCount();
  }
}
