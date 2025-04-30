package com.marketingproject.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.marketingproject.dtos.MessagingDataDto;
import com.marketingproject.dtos.request.AdvertisingAttachmentRequestDto;
import com.marketingproject.dtos.request.AttachmentRequestDto;
import com.marketingproject.dtos.request.ClientRequestDto;
import com.marketingproject.dtos.request.ContactRequestDto;
import com.marketingproject.dtos.response.ClientResponseDto;
import com.marketingproject.entities.Client;
import com.marketingproject.entities.Contact;
import com.marketingproject.entities.VerificationCode;
import com.marketingproject.enums.CodeType;
import com.marketingproject.enums.DefaultStatus;
import com.marketingproject.helpers.AttachmentHelper;
import com.marketingproject.helpers.ClientRequestHelper;
import com.marketingproject.infra.exceptions.BusinessRuleException;
import com.marketingproject.infra.exceptions.ForbiddenException;
import com.marketingproject.infra.exceptions.ResourceNotFoundException;
import com.marketingproject.infra.security.model.AuthenticatedUser;
import com.marketingproject.infra.security.model.PasswordRequestDto;
import com.marketingproject.infra.security.model.PasswordUpdateRequestDto;
import com.marketingproject.infra.security.services.AuthenticatedUserService;
import com.marketingproject.repositories.ClientRepository;
import com.marketingproject.services.ClientService;
import com.marketingproject.services.VerificationCodeService;
import com.marketingproject.shared.audit.CustomRevisionListener;
import com.marketingproject.shared.constants.SharedConstants;
import com.marketingproject.shared.constants.valitation.AuthValidationMessageConstants;
import com.marketingproject.shared.constants.valitation.ClientValidationMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {
    private final ClientRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final ClientRequestHelper helper;
    private final AttachmentHelper attachmentHelper;
    private final VerificationCodeService verificationCodeService;
    private final AuthenticatedUserService authenticatedUserService;

    @Override
    @Transactional
    public void save(ClientRequestDto request) {
        helper.validateClientRequest(request, null);

        Client client = new Client(request);
        VerificationCode verificationCode = verificationCodeService.save(CodeType.CONTACT, client);
        verificationCode.setValidated(true);
        client.setVerificationCode(verificationCode);

//        MessagingDataDto messagingData = new MessagingDataDto(client, verificationCode, client.getContact().getContactPreference());
//        verificationCodeService.send(messagingData, SharedConstants.TEMPLATE_EMAIL_CONTACT_VERIFICATION, SharedConstants.EMAIL_SUBJECT_CONTACT_VERIFICATION);

        repository.save(client);
    }


    @Override
    @Transactional(readOnly = true)
    public ClientResponseDto findById(UUID id) {
        return repository.findActiveById(id)
                .map(ClientResponseDto::new)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponseDto getDataFromToken() {
        UUID clientId = authenticatedUserService.getLoggedUser().client().getId();
        return repository.findActiveById(clientId)
                .map(ClientResponseDto::new)
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
            String hashedPass = passwordEncoder.encode(request.getPassword());
            client.setPassword(hashedPass);
            client.setStatus(DefaultStatus.ACTIVE);
            repository.save(client);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void sendResetPasswordCode(String identification) {
        helper.validateIdentification(identification);
        Client client = findActiveByIdentification(identification);

        VerificationCode verificationCode = verificationCodeService.save(CodeType.PASSWORD, client);
        client.setVerificationCode(verificationCode);
        repository.save(client);

        MessagingDataDto messagingData = new MessagingDataDto(client, verificationCode, client.getContact().getContactPreference());
        verificationCodeService.send(messagingData, SharedConstants.TEMPLATE_EMAIL_RESET_PASSWORD, SharedConstants.EMAIL_SUBJECT_RESET_PASSWORD);
    }

    @Override
    @Transactional
    public void resetPassword(String identificationNumber, PasswordRequestDto request) {
        request.validate();
        helper.validateIdentification(identificationNumber);
        Client client = findActiveByIdentification(identificationNumber);
        helper.verifyValidationCode(client);

        String hashedPass = passwordEncoder.encode(request.getPassword());
        client.setPassword(hashedPass);
        repository.save(client);
    }

    @Override
    @Transactional
    public void updatePassword(PasswordUpdateRequestDto request, AuthenticatedUser authClient) {
        Client client = authClient.client();
        helper.verifyValidationCode(client);

        if (!passwordEncoder.matches(request.getPassword(), authClient.getPassword())) {
            throw new BusinessRuleException(AuthValidationMessageConstants.INVALID_CREDENTIALS);
        }

        String hashedPass = passwordEncoder.encode(request.getPassword());
        client.setPassword(hashedPass);
        repository.save(client);
    }

    @Transactional
    @Override
    public void update(ClientRequestDto request, UUID id) throws JsonProcessingException {
        AuthenticatedUser authenticatedUser = authenticatedUserService.validateSelfOrAdmin(id);

        Client client = findEntityById(id);
        helper.validateClientRequest(request, client);

        CustomRevisionListener.setUsername(authenticatedUser.client().getBusinessName());
        CustomRevisionListener.setOldData(client.toStringMapper());

        client.update(request, authenticatedUser.client().getBusinessName());
        repository.save(client);
    }

    @Transactional
    @Override
    public void uploadAttachments(List<AttachmentRequestDto> request, UUID clientId) throws JsonProcessingException {
        attachmentHelper.validate(request);

        AuthenticatedUser authenticatedUser = authenticatedUserService.validateSelfOrAdmin(clientId);

        Client client = findEntityById(clientId);

        if (!client.getAttachments().isEmpty()) {
            CustomRevisionListener.setUsername(authenticatedUser.client().getBusinessName());
            CustomRevisionListener.setOldData(client.toStringMapper());

            attachmentHelper.removeAttachmentsNotSent(request, client);
        }

        attachmentHelper.saveAttachments(request, client);
        repository.save(client);
    }

    @Transactional
    @Override
    public void uploadAdvertisingAttachments(List<AdvertisingAttachmentRequestDto> request, UUID clientId) throws JsonProcessingException {
        attachmentHelper.validate(request);
        AuthenticatedUser authenticatedUser = authenticatedUserService.validateSelfOrAdmin(clientId);

        Client client = findEntityById(clientId);

        if (!client.getAdvertisingAttachments().isEmpty()) {
            CustomRevisionListener.setUsername(authenticatedUser.client().getBusinessName());
            CustomRevisionListener.setOldData(client.toStringMapper());

            attachmentHelper.removeAttachmentsNotSent(request, client);
        }

        attachmentHelper.saveAttachments(request, client);
        repository.save(client);
    }

    protected Client findActiveByIdentification(String identification) {
        return repository.findActiveByIdentificationNumber(identification)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
    }

    protected Client findEntityById(UUID id) {
        return repository.findActiveById(id).orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
    }


    protected Client findByIdentification(String identification) {
        return repository.findByIdentificationNumber(identification)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
    }
}
