package com.telas.services.ad;

import com.telas.dtos.request.AttachmentRequestDto;
import com.telas.dtos.response.ClientReferenceAttachmentAdminDto;
import com.telas.dtos.response.LinkResponseDto;
import com.telas.entities.Ad;
import com.telas.entities.AdRequest;
import com.telas.entities.Attachment;
import com.telas.entities.Client;
import com.telas.helpers.AdMediaLinkFactory;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.AdRepository;
import com.telas.repositories.AdRequestRepository;
import com.telas.repositories.AttachmentRepository;
import com.telas.repositories.ClientRepository;
import com.telas.services.BucketService;
import com.telas.services.BusinessQuestionnaireService;
import com.telas.shared.constants.valitation.AdValidationMessages;
import com.telas.shared.constants.valitation.AttachmentValidationMessages;
import com.telas.shared.utils.AttachmentUtils;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdUploadService {

    private final AttachmentRepository attachmentRepository;
    private final AdRepository adRepository;
    private final AdRequestRepository adRequestRepository;
    private final ClientRepository clientRepository;
    private final BucketService bucketService;
    private final AdMediaLinkFactory adMediaLinkFactory;
    private final BusinessQuestionnaireService businessQuestionnaireService;

    @Transactional
    public <T extends AttachmentRequestDto> void validate(List<T> requestList) {
        if (ValidateDataUtils.isNullOrEmpty(requestList)) {
            throw new BusinessRuleException(AttachmentValidationMessages.ATTACHMENT_LIST_EMPTY);
        }
        requestList.forEach(AttachmentRequestDto::validate);
    }

    public List<Attachment> getAttachmentsByIds(List<UUID> attachmentsIds) {
        return attachmentRepository.findByIdIn(attachmentsIds)
                .orElseThrow(() -> new BusinessRuleException(AttachmentValidationMessages.ATTACHMENT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<ClientReferenceAttachmentAdminDto> buildClientReferencesForAd(Ad ad) {
        AdRequest ar = ad.getAdRequest();
        if (ar == null || ValidateDataUtils.isNullOrEmptyString(ar.getAttachmentIds())) {
            return Collections.emptyList();
        }
        List<Attachment> attachments = getAttachmentsFromAdRequest(ar);
        Integer questionnaireVersion = businessQuestionnaireService.findLatestVersionByAdRequestId(ar.getId()).orElse(null);
        Instant questionnaireUpdatedAt =
                businessQuestionnaireService.findLatestRevisionCreatedAt(ar.getId()).orElse(null);
        return attachments.stream()
                .map(att -> new ClientReferenceAttachmentAdminDto(
                        att.getId(),
                        questionnaireVersion,
                        questionnaireUpdatedAt,
                        adMediaLinkFactory.getLink(att),
                        adMediaLinkFactory.getDownloadLink(att)))
                .toList();
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
        return adMediaLinkFactory.getLink(adEntity);
    }

    @Transactional(readOnly = true)
    public String getDownloadLinkFromAd(Ad adEntity) {
        return adMediaLinkFactory.getDownloadLink(adEntity);
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
            uploadMedia(request, attachment);
        });
        clientRepository.save(client);
    }

    @Transactional
    public void markReferenceAttachmentsConsumed(AdRequest adRequest) {
        if (adRequest == null || ValidateDataUtils.isNullOrEmptyString(adRequest.getAttachmentIds())) {
            return;
        }
        List<UUID> ids = Arrays.stream(adRequest.getAttachmentIds().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        List<Attachment> list = attachmentRepository.findAllById(ids);
        list.forEach(a -> a.setReferenceConsumed(true));
        attachmentRepository.saveAll(list);
    }

    @Transactional
    public void deleteClientAttachment(Client owner, UUID attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException(AttachmentValidationMessages.ATTACHMENT_NOT_FOUND));
        if (attachment.getClient() == null || !attachment.getClient().getId().equals(owner.getId())) {
            throw new ResourceNotFoundException(AttachmentValidationMessages.ATTACHMENT_NOT_FOUND);
        }
        if (adRepository.existsAdReferencingAttachment(attachmentId)) {
            throw new BusinessRuleException(AttachmentValidationMessages.ATTACHMENT_CANNOT_DELETE_REFERENCED);
        }
        AdRequest ar = owner.getAdRequest();
        if (ar != null && ar.isActive() && attachmentIdsCsvContains(ar.getAttachmentIds(), attachmentId)) {
            String updated = removeAttachmentIdFromCsv(ar.getAttachmentIds(), attachmentId);
            ar.setAttachmentIds(updated);
            adRequestRepository.save(ar);
        }
        bucketService.deleteAttachment(AttachmentUtils.format(attachment));
        owner.getAttachments().removeIf(a -> a.getId().equals(attachmentId));
        attachmentRepository.delete(attachment);
        clientRepository.save(owner);
    }

    public void uploadMedia(AttachmentRequestDto attachment, Object entity) {
        if (entity instanceof Ad || entity instanceof Attachment) {
            bucketService.upload(
                    attachment.getBytes(),
                    AttachmentUtils.format(entity),
                    attachment.getType(),
                    new ByteArrayInputStream(attachment.getBytes())
            );
        }
    }

    public void verifyFileNameChanged(AttachmentRequestDto request, Object entity) {
        String entityName = entity instanceof Attachment
                ? ((Attachment) entity).getName()
                : ((Ad) entity).getName();
        if (entityName.equals(request.getName())) {
            throw new BusinessRuleException(AdValidationMessages.FILE_NAME_MUST_BE_CHANGED_DURING_UPDATE);
        }
    }

    public Ad findAdById(UUID adId) {
        return adRepository.findById(adId)
                .orElseThrow(() -> new ResourceNotFoundException(AdValidationMessages.AD_NOT_FOUND));
    }

    public Ad replaceAdMedia(AttachmentRequestDto request, Ad ad) {
        bucketService.deleteAttachment(AttachmentUtils.format(ad));
        ad.setName(request.getName());
        ad.setType(request.getType());
        return ad;
    }

    public Ad persistAd(Ad ad) {
        return adRepository.save(ad);
    }

    public Ad createAdEntityFromRequest(AdRequest entity, AttachmentRequestDto request) {
        return new Ad(request, entity.getClient(), entity);
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

    private List<LinkResponseDto> getAttachmentsLinksResponseFromAdRequest(AdRequest adRequestEntity) {
        if (ValidateDataUtils.isNullOrEmptyString(adRequestEntity.getAttachmentIds())) {
            return Collections.emptyList();
        }
        List<Attachment> attachments = getAttachmentsFromAdRequest(adRequestEntity);
        return attachments.stream()
                .map(attachment -> new LinkResponseDto(
                        attachment.getId(),
                        attachment.getName(),
                        adMediaLinkFactory.getLink(attachment),
                        adMediaLinkFactory.getDownloadLink(attachment)))
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
                adMediaLinkFactory.getLink(ad),
                adMediaLinkFactory.getDownloadLink(ad));
    }

    private static String removeAttachmentIdFromCsv(String csv, UUID attachmentId) {
        if (ValidateDataUtils.isNullOrEmptyString(csv)) {
            return "";
        }
        String needle = attachmentId.toString();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(s -> !s.equals(needle))
                .collect(Collectors.joining(","));
    }

    private boolean attachmentIdsCsvContains(String csv, UUID attachmentId) {
        if (ValidateDataUtils.isNullOrEmptyString(csv)) {
            return false;
        }
        String needle = attachmentId.toString();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .anyMatch(needle::equals);
    }
}
