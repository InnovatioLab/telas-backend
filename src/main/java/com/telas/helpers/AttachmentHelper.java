package com.telas.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telas.dtos.request.AdRequestDto;
import com.telas.dtos.request.AttachmentRequestDto;
import com.telas.dtos.request.RefusedAdRequestDto;
import com.telas.dtos.response.LinkResponseDto;
import com.telas.entities.*;
import com.telas.enums.AdValidationType;
import com.telas.enums.NotificationReference;
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


  public List<Attachment> getAttachmentsFromAdRequest(AdRequest adRequestEntity) {
    List<UUID> attachmentIds = Arrays.stream(adRequestEntity.getAttachmentIds().split(","))
            .map(UUID::fromString)
            .toList();

    return getAttachmentsByIds(attachmentIds);
  }

  @Transactional
  public <T extends AttachmentRequestDto> void saveAttachments(List<T> requestList, Client client, AdRequest adRequestEntity) {
    requestList.forEach(attachment -> {
      if (attachment.getId() == null) {
        if ((attachment instanceof AdRequestDto adRequest) && adRequestEntity != null) {
          List<Attachment> attachments = getAttachmentsFromAdRequest(adRequestEntity);

          Client adOwner = adRequestEntity.getClient();

          Ad ad = adRequestEntity.getAd() == null
                  ? new Ad(adRequest, adOwner, adRequestEntity)
                  : adRequestEntity.getAd();

          if (adRequestEntity.getAd() == null) {
            ad.getAttachments().addAll(attachments);
            adOwner.getAds().add(ad);
          } else {
            bucketService.deleteAttachment(AttachmentUtils.format(ad));
            ad.setName(attachment.getName());
            ad.setType(attachment.getType());
            ad.setValidation(AdValidationType.PENDING);
          }

          adRequestEntity.closeRequest();
          adRequestRepository.save(adRequestEntity);

          Ad savedAd = adRepository.save(ad);
          String fileName = AttachmentUtils.format(savedAd);
          bucketService.upload(attachment.getBytes(), fileName, attachment.getType(), new ByteArrayInputStream(attachment.getBytes()));
        } else if (!(attachment instanceof AdRequestDto) && adRequestEntity == null) {
          Attachment newAttachment = new Attachment(attachment, client);
          Attachment savedAttachment = attachmentRepository.save(newAttachment);
          client.getAttachments().add(savedAttachment);

          String fileName = AttachmentUtils.format(savedAttachment);
          bucketService.upload(attachment.getBytes(), fileName, attachment.getType(), new ByteArrayInputStream(attachment.getBytes()));
        }
      } else {
        if (!(attachment instanceof AdRequestDto)) {
          Attachment entity = attachmentRepository.findById(attachment.getId()).orElseThrow(() -> new ResourceNotFoundException(AttachmentValidationMessages.ATTACHMENT_NOT_FOUND));
          bucketService.deleteAttachment(AttachmentUtils.format(entity));
          entity.setName(attachment.getName());
          entity.setType(attachment.getType());

          Attachment savedAttachment = attachmentRepository.save(entity);

          String newFileName = AttachmentUtils.format(savedAttachment);
          bucketService.upload(attachment.getBytes(), newFileName, savedAttachment.getType(), new ByteArrayInputStream(attachment.getBytes()));
        }
      }
    });
  }

//  @Transactional
//  public <T extends AttachmentRequestDto> void removeAttachmentsNotSent(List<T> requestList, Client client) {
//    List<UUID> attachmentIds = requestList.stream()
//            .map(AttachmentRequestDto::getId)
//            .filter(Objects::nonNull)
//            .toList();
//
//    boolean isAd = requestList.stream().allMatch(AdRequestDto.class::isInstance);
//
//    if (isAd) {
//      List<Ad> adsToDelete = client.getAds().stream()
//              .filter(attachment -> !attachmentIds.contains(attachment.getId()))
//              .toList();
//
//      removeAds(adsToDelete, client);
//    }
//  }

//  void removeAds(List<Ad> adsToDelete, Client client) {
//    adsToDelete.forEach(ad -> {
//      client.getAds().remove(ad);
//      ad.getAttachments().clear();
//      removeAdsFromMonitors(ad);
//      bucketService.deleteAttachment(AttachmentUtils.format(ad));
//    });
//
//    adRepository.deleteAll(adsToDelete);
//  }

//  void removeAdsFromMonitors(Ad ad) {
//    List<Monitor> monitors = monitorRepository.findByAdId(ad.getId());
//
//    monitors.forEach(monitor ->
//            monitor.getMonitorAds()
//                    .removeIf(attachment -> attachment.getAd().equals(ad))
//    );
//
//    monitorAdRepository.deleteAll(
//            monitorAdRepository.findByAdId(ad.getId())
//    );
//  }

  @Transactional
  public void validateAd(Ad entity, AdValidationType validation, RefusedAdRequestDto request, Client client) throws JsonProcessingException {
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
    CustomRevisionListener.setOldData(entity.toStringMapper());
    entity.setValidation(validation);

    if (AdValidationType.REJECTED.equals(validation)) {
      if (request == null) {
        throw new BusinessRuleException(AttachmentValidationMessages.JUSTIFICATION_REQUIRED);
      }
      createRefusedAd(request, entity, client);
//      removeAdsFromMonitors(entity);
    }

    adRequestRepository.save(entity.getAdRequest());

    if (AdValidationType.REJECTED.equals(validation)) {
      clientRepository.findAllAdmins().forEach(admin ->
              sendNotification(entity, admin, validation, request)
      );
    }
  }

  private void sendNotification(Ad entity, Client admin, AdValidationType validation, RefusedAdRequestDto request) {
    Map<String, String> params = new HashMap<>();

    String link = "";
    params.put("link", link);
    params.put("attachmentName", entity.getName());
    params.put("recipient", admin.getBusinessName());

    if (AdValidationType.REJECTED.equals(validation)) {
      params.put("justification", request.getJustification());
      params.put("description", Optional.ofNullable(request.getDescription()).orElse(""));
    } else {
      params.put("justification", "");
      params.put("description", "");
    }

//        NotificationReference notificationReference = AdValidationType.APPROVED.equals(validation) ? NotificationReference.ATTACHMENT_APPROVED : NotificationReference.ATTACHMENT_REFUSED;
    notificationService.save(NotificationReference.ATTACHMENT_REFUSED, admin, params);
//        notificationService.notify(notificationReference, client, params);
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
