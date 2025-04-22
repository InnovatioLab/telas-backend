package com.marketingproject.helpers;

import com.marketingproject.dtos.request.ClientRequestDto;
import com.marketingproject.entities.Client;
import com.marketingproject.infra.exceptions.BusinessRuleException;
import com.marketingproject.repositories.ClientRepository;
import com.marketingproject.repositories.ContactRepository;
import com.marketingproject.repositories.OwnerRepository;
import com.marketingproject.shared.constants.valitation.ClientValidationMessages;
import com.marketingproject.shared.constants.valitation.OwnerValidationMessages;
import com.marketingproject.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ClientRequestHelper {
    private final OwnerRepository ownerRepository;
    private final ClientRepository clientRepository;
    private final ContactRepository contactRepository;

    @Transactional(readOnly = true)
    public void validateClientRequest(ClientRequestDto request) {
        request.validate();
        verifyUniqueIdentificationNumber(request);
        verifyUniqueEmail(request);
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

    void verifyUniqueEmail(ClientRequestDto request) {
        if (ownerRepository.existsByEmail(request.getOwner().getEmail())) {
            throw new BusinessRuleException(OwnerValidationMessages.EMAIL_UNIQUE);
        }

        if (contactRepository.existsByEmail(request.getContact().getEmail())) {
            throw new BusinessRuleException(ClientValidationMessages.EMAIL_UNIQUE);
        }
    }

    void verifyUniqueIdentificationNumber(ClientRequestDto request) {
        if (ownerRepository.existsByIdentificationNumber(request.getOwner().getIdentificationNumber())) {
            throw new BusinessRuleException(OwnerValidationMessages.IDENTIFICATION_NUMBER_UNIQUE);
        }

        if (clientRepository.existsByIdentificationNumber(request.getIdentificationNumber())) {
            throw new BusinessRuleException(ClientValidationMessages.IDENTIFICATION_NUMBER_UNIQUE);
        }
    }
}
