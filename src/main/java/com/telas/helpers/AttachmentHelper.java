package com.telas.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
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


  @Transactional
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

          Ad ad = Optional.ofNullable(adRequestEntity.getAd())
                  .orElseGet(() -> {
                    Ad newAd = new Ad(adRequest, adOwner, adRequestEntity);
                    newAd.getAttachments().addAll(attachments);
                    adOwner.getAds().add(newAd);
                    return newAd;
                  });

          if (adRequestEntity.getAd() != null) {
            bucketService.deleteAttachment(AttachmentUtils.format(ad));
            ad.setName(attachment.getName());
            ad.setType(attachment.getType());
            ad.setValidation(AdValidationType.PENDING);
          }

          if (Role.ADMIN.equals(client.getRole())) {
            ad.setValidation(AdValidationType.APPROVED);
          }

          adRequestEntity.closeRequest();
          adRequestRepository.save(adRequestEntity);

          Ad savedAd = adRepository.save(ad);
          bucketService.upload(
                  attachment.getBytes(),
                  AttachmentUtils.format(savedAd),
                  attachment.getType(),
                  new ByteArrayInputStream(attachment.getBytes())
          );
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
          verifyFileNameChanged(attachment, entity);
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

  private void verifyFileNameChanged(AttachmentRequestDto request, Attachment entity) {
    if (entity.getName().equals(request.getName())) {
      throw new BusinessRuleException(AdValidationMessages.FILE_NAME_MUST_BE_CHANGED_DURING_UPDATE);
    }
  }

  @Transactional
  public void saveAdminAds(AdRequestDto request, Client admin) {
    Ad ad = request.getId() == null ? createNewAd(request, admin) : updateExistingAd(request);
    bucketService.upload(
            request.getBytes(),
            AttachmentUtils.format(ad),
            request.getType(),
            new ByteArrayInputStream(request.getBytes())
    );
  }

  private Ad createNewAd(AdRequestDto request, Client admin) {
    Ad ad = new Ad();
    ad.setName(request.getName());
    ad.setType(request.getType());
    ad.setClient(admin);
    ad.setValidation(AdValidationType.APPROVED);

    Ad savedAd = adRepository.save(ad);
    admin.getAds().add(savedAd);
    clientRepository.save(admin);
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

  private void verifyFileNameChanged(AdRequestDto request, Ad ad) {
    if (ad.getName().equals(request.getName())) {
      throw new BusinessRuleException(AdValidationMessages.FILE_NAME_MUST_BE_CHANGED_DURING_UPDATE);
    }
  }

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
    }

    adRequestRepository.save(entity.getAdRequest());

    if (AdValidationType.REJECTED.equals(validation)) {
      clientRepository.findAllAdmins().forEach(admin ->
              sendNotification(entity, admin, request)
      );
    }
  }

  private void sendNotification(Ad entity, Client admin, RefusedAdRequestDto request) {
    Map<String, String> params = new HashMap<>();

    String link = "";
    params.put("link", link);
    params.put("attachmentName", entity.getName());
    params.put("recipient", admin.getBusinessName());
    params.put("justification", request.getJustification());
    params.put("description", Optional.ofNullable(request.getDescription()).orElse(""));
    notificationService.save(NotificationReference.AD_REFUSED, admin, params);
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
