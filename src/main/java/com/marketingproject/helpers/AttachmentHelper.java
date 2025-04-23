package com.marketingproject.helpers;

import com.marketingproject.dtos.request.AdvertisingAttachmentRequestDto;
import com.marketingproject.dtos.request.AttachmentRequestDto;
import com.marketingproject.entities.AdvertisingAttachment;
import com.marketingproject.entities.Attachment;
import com.marketingproject.entities.Client;
import com.marketingproject.infra.exceptions.BusinessRuleException;
import com.marketingproject.repositories.AdvertisingAttachmentRepository;
import com.marketingproject.repositories.AttachmentRepository;
import com.marketingproject.services.BucketService;
import com.marketingproject.shared.constants.valitation.AttachmentValidationMessages;
import com.marketingproject.shared.utils.AttachmentUtils;
import com.marketingproject.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AttachmentHelper {
    private final AttachmentRepository attachmentRepository;
    private final AdvertisingAttachmentRepository advertisingAttachmentRepository;
    private final BucketService bucketService;

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
    public <T extends AttachmentRequestDto> void saveAttachments(List<T> requestList, Client client) {
        requestList.forEach(attachment -> {
            if (attachment.getId() == null) {
                if (attachment instanceof AdvertisingAttachmentRequestDto advertisingAttachment) {
                    List<Attachment> attachments = getAttachmentsByIds(advertisingAttachment.getAttachmentIds());
                    AdvertisingAttachment newAttachment = new AdvertisingAttachment(advertisingAttachment, client);
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
                    .filter(attachment -> attachmentIds.contains(attachment.getId()))
                    .toList();

            client.getAdvertisingAttachments().removeAll(advertisingAttachmentsToDelete);
            removeAdvertisingAttachments(advertisingAttachmentsToDelete, client);
        } else {
            List<Attachment> attachmentsToDelete = client.getAttachments().stream()
                    .filter(attachment -> attachmentIds.contains(attachment.getId()))
                    .toList();

            client.getAttachments().removeAll(attachmentsToDelete);
            removeAttachment(attachmentsToDelete, client);
        }
    }

    void removeAttachment(List<Attachment> attachmentsToDelete, Client client) {
        // Remove os relacionamentos na tabela pivô advertising_attachments_attachments
        attachmentsToDelete.forEach(attachment -> {
            client.getAdvertisingAttachments().forEach(advertisingAttachment ->
                    advertisingAttachment.getAttachments().remove(attachment)
            );
            bucketService.deleteAttachment(AttachmentUtils.format(attachment));
        });

        client.getAttachments().removeAll(attachmentsToDelete);
        attachmentRepository.deleteAll(attachmentsToDelete);
    }

    void removeAdvertisingAttachments(List<AdvertisingAttachment> advertisingAttachmentsToDelete, Client client) {
        // Remove os relacionamentos na tabela pivô monitor_advertising_attachments
        advertisingAttachmentsToDelete.forEach(advertisingAttachment -> {
            advertisingAttachment.getClient().getAdvertisingAttachments().remove(advertisingAttachment);
            advertisingAttachment.getAttachments().clear();
            bucketService.deleteAttachment(AttachmentUtils.format(advertisingAttachment));
        });

        // Remove os AdvertisingAttachments da lista de AdvertisingAttachments do Client
        client.getAdvertisingAttachments().removeAll(advertisingAttachmentsToDelete);

        // Remove os relacionamentos na entidade Monitors

        advertisingAttachmentRepository.deleteAll(advertisingAttachmentsToDelete);
    }
}
