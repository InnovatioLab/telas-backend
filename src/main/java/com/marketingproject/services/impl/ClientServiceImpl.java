package com.marketingproject.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.marketingproject.dtos.MessagingDataDto;
import com.marketingproject.dtos.request.ClientRequestDto;
import com.marketingproject.dtos.request.ContactRequestDto;
import com.marketingproject.dtos.request.PasswordRequestDto;
import com.marketingproject.entities.Client;
import com.marketingproject.entities.Contact;
import com.marketingproject.entities.VerificationCode;
import com.marketingproject.enums.CodeType;
import com.marketingproject.enums.DefaultStatus;
import com.marketingproject.helpers.ClientRequestHelper;
import com.marketingproject.infra.exceptions.ForbiddenException;
import com.marketingproject.infra.exceptions.ResourceNotFoundException;
import com.marketingproject.infra.security.services.AuthenticatedUserService;
import com.marketingproject.repositories.ClientRepository;
import com.marketingproject.services.ClientService;
import com.marketingproject.services.VerificationCodeService;
import com.marketingproject.shared.audit.CustomRevisionListener;
import com.marketingproject.shared.constants.SharedConstants;
import com.marketingproject.shared.constants.valitation.AuthValidationMessageConstants;
import com.marketingproject.shared.constants.valitation.ClientValidationMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {
    private final ClientRepository repository;
    private final ClientRequestHelper helper;
    private final VerificationCodeService verificationCodeService;
    private final AuthenticatedUserService authenticatedUserService;

    @Override
    @Transactional
    public void save(ClientRequestDto request) {
        helper.validateClientRequest(request);

        Client client = new Client(request);
        VerificationCode verificationCode = verificationCodeService.save(CodeType.CONTACT, client);
        client.setVerificationCode(verificationCode);

        MessagingDataDto messagingData = new MessagingDataDto(client, verificationCode, client.getContact().getContactPreference());
        verificationCodeService.send(messagingData, SharedConstants.TEMPLATE_EMAIL_CONTACT_VERIFICATION, SharedConstants.EMAIL_SUBJECT_CONTACT_VERIFICATION);

        repository.save(client);
    }

    @Override
    public Client findActiveByIdentification(String identification) {
        return repository.findActiveByIdentificationNumber(identification)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
    }

    @Override
    @Transactional
    public void validateCode(String identification, String codigo) {
        helper.validateIdentification(identification);
        Client client = findByIdentification(identification);
        verificationCodeService.validate(client, codigo);
        repository.save(client);
    }

    @Override
    @Transactional(readOnly = true)
    public void resendCode(String identification) {
        helper.validateIdentification(identification);
        Client client = findByIdentification(identification);
        VerificationCode verificationCode = verificationCodeService.save(CodeType.CONTACT, client);
        client.setVerificationCode(verificationCode);
        repository.save(client);

        MessagingDataDto messagingData = new MessagingDataDto(client, verificationCode, client.getContact().getContactPreference());
        verificationCodeService.send(messagingData, SharedConstants.TEMPLATE_EMAIL_CONTACT_VERIFICATION, SharedConstants.EMAIL_SUBJECT_CONTACT_VERIFICATION);
    }

    @Override
    @Transactional
    public void updateContact(String identification, ContactRequestDto request) throws JsonProcessingException {
        helper.validateIdentification(identification);
        request.validate();
        Client client = findByIdentification(identification);

        if (DefaultStatus.ACTIVE.equals(client.getStatus())) {
            throw new ForbiddenException(AuthValidationMessageConstants.ERROR_NO_PERMISSION);
        }

        Contact contact = client.getContact();
        boolean changes = contact.validateChangesUpdateCode(request);

        if (changes) {
            boolean emailChanged = request.getEmail() != null && !contact.getEmail().equals(request.getEmail());

            if (emailChanged) {
                helper.verifyUniqueEmail(request.getEmail());

            }

            CustomRevisionListener.setUsername(client.getBusinessName());
            CustomRevisionListener.setOldData(client.toStringMapper());
            contact.applyChangesUpdateCode(request);
        }

        VerificationCode verificationCode = verificationCodeService.save(CodeType.CONTACT, client);
        client.setVerificationCode(verificationCode);
        repository.save(client);

        MessagingDataDto messagingData = new MessagingDataDto(client, verificationCode, client.getContact().getContactPreference());
        verificationCodeService.send(messagingData, SharedConstants.TEMPLATE_EMAIL_CONTACT_VERIFICATION, SharedConstants.EMAIL_SUBJECT_CONTACT_VERIFICATION);
    }

    @Override
    @Transactional
    public void createPassword(String identification, PasswordRequestDto request) {
        request.validate();
        helper.validateIdentification(identification);
        Client client = findByIdentification(identification);

        if (!DefaultStatus.ACTIVE.equals(client.getStatus())) {
            helper.verifyValidationCode(client);
//            Criar a senha com hash
            client.setStatus(DefaultStatus.ACTIVE);
            repository.save(client);
        }

    }

    @Override
    @Transactional(readOnly = true)
    public void resetPassword(PasswordRequestDto request) {
        request.validate();
        Client client = authenticatedUserService.getLoggedUser().client();

        if (DefaultStatus.ACTIVE.equals(client.getStatus()) && CodeType.PASSWORD.equals(client.getVerificationCode().getCodeType())) {
            helper.verifyValidationCode(client);
            //            Atualizar a senha com hash
            repository.save(client);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void sendResetPasswordCode(Client client) {
        VerificationCode verificationCode = verificationCodeService.save(CodeType.PASSWORD, client);
        client.setVerificationCode(verificationCode);
        repository.save(client);

        MessagingDataDto messagingData = new MessagingDataDto(client, verificationCode, client.getContact().getContactPreference());
        verificationCodeService.send(messagingData, SharedConstants.TEMPLATE_EMAIL_RESET_PASSWORD, SharedConstants.EMAIL_SUBJECT_RESET_PASSWORD);
    }

    protected Client findByIdentification(String identification) {
        return repository.findByIdentificationNumber(identification)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
    }
}
