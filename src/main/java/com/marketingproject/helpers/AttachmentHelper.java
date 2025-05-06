package com.marketingproject.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.marketingproject.dtos.request.AdvertisingAttachmentRequestDto;
import com.marketingproject.dtos.request.AttachmentRequestDto;
import com.marketingproject.dtos.request.RefusedAttachmentRequestDto;
import com.marketingproject.entities.*;
import com.marketingproject.enums.AttachmentValidationType;
import com.marketingproject.enums.NotificationReference;
import com.marketingproject.infra.exceptions.BusinessRuleException;
import com.marketingproject.infra.exceptions.ResourceNotFoundException;
import com.marketingproject.repositories.AdvertisingAttachmentRepository;
import com.marketingproject.repositories.AttachmentRepository;
import com.marketingproject.repositories.MonitorAdvertisingAttachmentRepository;
import com.marketingproject.repositories.MonitorRepository;
import com.marketingproject.services.BucketService;
import com.marketingproject.services.NotificationService;
import com.marketingproject.shared.audit.CustomRevisionListener;
import com.marketingproject.shared.constants.valitation.AttachmentValidationMessages;
import com.marketingproject.shared.utils.AttachmentUtils;
import com.marketingproject.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.*;

@Component
@RequiredArgsConstructor
public class AttachmentHelper {
    private final AttachmentRepository attachmentRepository;
    private final AdvertisingAttachmentRepository advertisingAttachmentRepository;
    private final MonitorAdvertisingAttachmentRepository monitorAdvertisingAttachmentRepository;
    private final MonitorRepository monitorRepository;
    private final BucketService bucketService;
    private final NotificationService notificationService;

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

    @Transactional
    public <T extends AttachmentRequestDto> void saveAttachments(List<T> requestList, Client client, boolean isAdmin) {
        requestList.forEach(attachment -> {
            if (attachment.getId() == null) {
                if (attachment instanceof AdvertisingAttachmentRequestDto advertisingAttachment) {
                    List<Attachment> attachments = getAttachmentsByIds(advertisingAttachment.getAttachmentIds());
                    AdvertisingAttachment newAttachment = new AdvertisingAttachment(advertisingAttachment, client);

                    if (isAdmin) {
                        newAttachment.setValidation(AttachmentValidationType.APPROVED);
                    }

                    newAttachment.getAttachments().addAll(attachments);

                    AdvertisingAttachment savedAttachment = advertisingAttachmentRepository.save(newAttachment);
                    client.getAdvertisingAttachments().add(savedAttachment);

                    String fileName = AttachmentUtils.format(savedAttachment);
                    bucketService.upload(attachment.getBytes(), fileName, attachment.getType(), new ByteArrayInputStream(attachment.getBytes()));

                } else {
                    Attachment newAttachment = new Attachment(attachment, client);
                    Attachment savedAttachment = attachmentRepository.save(newAttachment);
                    client.getAttachments().add(savedAttachment);

                    String fileName = AttachmentUtils.format(savedAttachment);
                    bucketService.upload(attachment.getBytes(), fileName, attachment.getType(), new ByteArrayInputStream(attachment.getBytes()));
                }
            } else {
                if (!(attachment instanceof AdvertisingAttachmentRequestDto)) {
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

    @Transactional
    public <T extends AttachmentRequestDto> void removeAttachmentsNotSent(List<T> requestList, Client client) {
        List<UUID> attachmentIds = requestList.stream()
                .map(AttachmentRequestDto::getId)
                .filter(Objects::nonNull)
                .toList();

        boolean isAdvertisingAttachment = requestList.stream().allMatch(AdvertisingAttachmentRequestDto.class::isInstance);

        if (isAdvertisingAttachment) {
            List<AdvertisingAttachment> advertisingAttachmentsToDelete = client.getAdvertisingAttachments().stream()
                    .filter(attachment -> !attachmentIds.contains(attachment.getId()))
                    .toList();

            removeAdvertisingAttachments(advertisingAttachmentsToDelete, client);
        }
    }

//    void removeAttachment(List<Attachment> attachmentsToDelete, Client client) {
//        attachmentsToDelete.forEach(attachment -> {
//            List<AdvertisingAttachment> toRemove = new ArrayList<>();
//
//            client.getAdvertisingAttachments().forEach(advertisingAttachment -> {
//                advertisingAttachment.getAttachments().remove(attachment);
//
//                if (advertisingAttachment.getAttachments().isEmpty()) {
//                    removeAdvertisingAttachmentFromMonitors(advertisingAttachment);
//                    toRemove.add(advertisingAttachment);
//                }
//            });
//
//            toRemove.forEach(client.getAdvertisingAttachments()::remove);
//            advertisingAttachmentRepository.deleteAll(toRemove);
//
//            bucketService.deleteAttachment(AttachmentUtils.format(attachment));
//        });
//
//        attachmentsToDelete.forEach(client.getAttachments()::remove);
//        attachmentRepository.deleteAll(attachmentsToDelete);
//    }

    void removeAdvertisingAttachments(List<AdvertisingAttachment> advertisingAttachmentsToDelete, Client client) {
        advertisingAttachmentsToDelete.forEach(advertisingAttachment -> {
            client.getAdvertisingAttachments().remove(advertisingAttachment);
            advertisingAttachment.getAttachments().clear();
            removeAdvertisingAttachmentFromMonitors(advertisingAttachment);
            bucketService.deleteAttachment(AttachmentUtils.format(advertisingAttachment));
        });

        advertisingAttachmentRepository.deleteAll(advertisingAttachmentsToDelete);
    }

    void removeAdvertisingAttachmentFromMonitors(AdvertisingAttachment advertisingAttachment) {
        List<Monitor> monitors = monitorRepository.findByAdvertisingAttachmentId(advertisingAttachment.getId());

        monitors.forEach(monitor ->
                monitor.getMonitorAdvertisingAttachments()
                        .removeIf(attachment -> attachment.getAdvertisingAttachment().equals(advertisingAttachment))
        );

        monitorAdvertisingAttachmentRepository.deleteAll(
                monitorAdvertisingAttachmentRepository.findByAdvertisingAttachmentId(advertisingAttachment.getId())
        );
    }

    @Transactional
    public void validateAttachment(UUID attachmentId, AttachmentValidationType validation, RefusedAttachmentRequestDto request, Client admin) throws JsonProcessingException {
        if (AttachmentValidationType.PENDING.equals(validation)) {
            throw new BusinessRuleException(AttachmentValidationMessages.PENDING_VALIDATION_NOT_ACCEPTED);
        }

        AdvertisingAttachment entity = advertisingAttachmentRepository.findById(attachmentId).orElseThrow(() -> new ResourceNotFoundException(AttachmentValidationMessages.ATTACHMENT_NOT_FOUND));
        CustomRevisionListener.setUsername(admin.getBusinessName());
        CustomRevisionListener.setOldData(entity.toStringMapper());
        entity.setValidation(validation);

        if (AttachmentValidationType.REJECTED.equals(validation)) {
            if (request == null) {
                throw new BusinessRuleException(AttachmentValidationMessages.JUSTIFICATION_REQUIRED);
            }
            createRefusedAttachment(request, entity, admin);
            removeAdvertisingAttachmentFromMonitors(entity);
        }

        advertisingAttachmentRepository.save(entity);

        sendNotification(entity, validation, request);
    }

    private void sendNotification(AdvertisingAttachment entity, AttachmentValidationType validation, RefusedAttachmentRequestDto request) {
        Client client = entity.getClient();
        Map<String, String> params = new HashMap<>();

        String link = "";
        params.put("link", link);
        params.put("attachmentName", entity.getName());
        params.put("recipient", client.getBusinessName());

        if (AttachmentValidationType.REJECTED.equals(validation)) {
            params.put("justification", request.getJustification());
            params.put("description", Optional.ofNullable(request.getDescription()).orElse(""));
        } else {
            params.put("justification", "");
            params.put("description", "");
        }

        NotificationReference notificationReference = AttachmentValidationType.APPROVED.equals(validation) ? NotificationReference.ATTACHMENT_APPROVED : NotificationReference.ATTACHMENT_REFUSED;
        notificationService.save(notificationReference, client, params);
        notificationService.notify(notificationReference, client, params);
    }

    private void createRefusedAttachment(RefusedAttachmentRequestDto request, AdvertisingAttachment entity, Client admin) {
        RefusedAttachment refusedAttachment = new RefusedAttachment(request, entity, admin);
        refusedAttachment.setUsernameCreate(admin.getBusinessName());
        entity.setRefusedAttachment(refusedAttachment);
        entity.setUsernameUpdate(admin.getBusinessName());
    }
}
