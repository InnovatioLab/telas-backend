package com.telas.helpers;

import com.telas.dtos.request.ClientAdRequestToAdminDto;
import com.telas.dtos.request.ClientRequestDto;
import com.telas.entities.Ad;
import com.telas.entities.AdRequest;
import com.telas.entities.Attachment;
import com.telas.entities.Client;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.repositories.*;
import com.telas.shared.constants.valitation.AdValidationMessages;
import com.telas.shared.constants.valitation.AttachmentValidationMessages;
import com.telas.shared.constants.valitation.ClientValidationMessages;
import com.telas.shared.constants.valitation.OwnerValidationMessages;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.module.ResolutionException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClientRequestHelper {
    private final OwnerRepository ownerRepository;
    private final ClientRepository clientRepository;
    private final ContactRepository contactRepository;
    private final AttachmentRepository attachmentRepository;
    private final AdRepository adRepository;
    private final AdRequestRepository adRequestRepository;

    @Transactional(readOnly = true)
    public void validateClientRequest(ClientRequestDto request, Client client) {
        request.validate();
        verifyUniqueIdentificationNumber(request, client);
        verifyUniqueEmail(request, client);
    }


    @Transactional(readOnly = true)
    public void verifyUniqueEmail(String email) {
        if (contactRepository.existsByEmail(email)) {
            throw new BusinessRuleException(ClientValidationMessages.EMAIL_UNIQUE);
        }
    }

    @Transactional
    public void verifyValidationCode(Client client) {
        if (!client.getVerificationCode().isValidated()) {
            throw new BusinessRuleException(ClientValidationMessages.VALIDATION_CODE_NOT_VALIDATED);
        }
    }

    @Transactional(readOnly = true)
    public void validateIdentification(String identification) {
        if (ValidateDataUtils.isNullOrEmptyString(identification)) {
            throw new BusinessRuleException(OwnerValidationMessages.IDENTIFICATION_NUMBER_REQUIRED);
        }

        if (!ValidateDataUtils.containsOnlyNumbers(identification)) {
            throw new BusinessRuleException(OwnerValidationMessages.IDENTIFICATION_NUMBER_REGEX);
        }

        if (identification.length() != 9) {
            throw new BusinessRuleException(OwnerValidationMessages.IDENTIFICATION_NUMBER_SIZE);
        }
    }

    @Transactional
    public void createAdRequest(ClientAdRequestToAdminDto request, Client client) {
        List<Attachment> attachments = getAttachmentsByIds(request.getAttachmentIds());
        AdRequest adRequest = new AdRequest(request, client, attachments);
        adRequestRepository.save(adRequest);
    }

    @Transactional(readOnly = true)
    public AdRequest getAdRequestById(UUID adRequestId) {
        return adRequestRepository.findById(adRequestId)
                .orElseThrow(() -> new ResolutionException(AdValidationMessages.AD_REQUEST_NOT_FOUND));
    }


    List<Attachment> getAttachmentsByIds(List<UUID> attachmentsIds) {
        return attachmentRepository.findByIdIn(attachmentsIds).orElseThrow(() -> new ResolutionException(AttachmentValidationMessages.ATTACHMENTS_NOT_FOUND));
    }

    void verifyUniqueEmail(ClientRequestDto request, Client client) {
        String newEmail = request.getContact().getEmail();
        String ownerEmail = request.getOwner().getEmail();

        if (client != null) {
            if (isEmailChanged(client.getContact().getEmail(), newEmail) && contactRepository.existsByEmail(newEmail)) {
                throw new BusinessRuleException(ClientValidationMessages.EMAIL_UNIQUE);
            }
            if (isEmailChanged(client.getOwner().getEmail(), ownerEmail) && ownerRepository.existsByEmail(ownerEmail)) {
                throw new BusinessRuleException(ClientValidationMessages.EMAIL_UNIQUE);
            }
        } else {
            if (ownerRepository.existsByEmail(ownerEmail)) {
                throw new BusinessRuleException(OwnerValidationMessages.EMAIL_UNIQUE);
            }
            if (contactRepository.existsByEmail(newEmail)) {
                throw new BusinessRuleException(ClientValidationMessages.EMAIL_UNIQUE);
            }
        }
    }

    void verifyUniqueIdentificationNumber(ClientRequestDto request, Client client) {
        String newIdNumber = request.getIdentificationNumber();
        String ownerIdNumber = request.getOwner().getIdentificationNumber();

        if (client != null) {
            if (isIdNumberChanged(client.getOwner().getIdentificationNumber(), ownerIdNumber) && ownerRepository.existsByIdentificationNumber(ownerIdNumber)) {
                throw new BusinessRuleException(ClientValidationMessages.EMAIL_UNIQUE);
            }
            if (isIdNumberChanged(client.getIdentificationNumber(), newIdNumber) && clientRepository.existsByIdentificationNumber(newIdNumber)) {
                throw new BusinessRuleException(ClientValidationMessages.EMAIL_UNIQUE);
            }
        } else {
            if (ownerRepository.existsByIdentificationNumber(ownerIdNumber)) {
                throw new BusinessRuleException(OwnerValidationMessages.IDENTIFICATION_NUMBER_UNIQUE);
            }
            if (clientRepository.existsByIdentificationNumber(newIdNumber)) {
                throw new BusinessRuleException(ClientValidationMessages.IDENTIFICATION_NUMBER_UNIQUE);
            }
        }
    }

    private boolean isEmailChanged(String currentEmail, String newEmail) {
        return !currentEmail.equals(newEmail);
    }

    private boolean isIdNumberChanged(String currentId, String newId) {
        return !currentId.equals(newId);
    }

    @Transactional(readOnly = true)
    public Ad getAdById(UUID adId) {
        return adRepository.findById(adId).orElseThrow(() -> new ResolutionException(AdValidationMessages.AD_NOT_FOUND));
    }
}
