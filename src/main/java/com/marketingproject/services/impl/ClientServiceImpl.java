package com.marketingproject.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.marketingproject.dtos.MessagingDataDto;
import com.marketingproject.dtos.request.*;
import com.marketingproject.dtos.request.filters.ClientFilterRequestDto;
import com.marketingproject.dtos.request.filters.FilterPendingAttachmentRequestDto;
import com.marketingproject.dtos.response.*;
import com.marketingproject.entities.*;
import com.marketingproject.enums.AttachmentValidationType;
import com.marketingproject.enums.CodeType;
import com.marketingproject.enums.DefaultStatus;
import com.marketingproject.enums.Role;
import com.marketingproject.helpers.AttachmentHelper;
import com.marketingproject.helpers.ClientRequestHelper;
import com.marketingproject.infra.exceptions.BusinessRuleException;
import com.marketingproject.infra.exceptions.ForbiddenException;
import com.marketingproject.infra.exceptions.ResourceNotFoundException;
import com.marketingproject.infra.security.model.AuthenticatedUser;
import com.marketingproject.infra.security.model.PasswordRequestDto;
import com.marketingproject.infra.security.model.PasswordUpdateRequestDto;
import com.marketingproject.infra.security.services.AuthenticatedUserService;
import com.marketingproject.repositories.AdvertisingAttachmentRepository;
import com.marketingproject.repositories.ClientRepository;
import com.marketingproject.services.BucketService;
import com.marketingproject.services.ClientService;
import com.marketingproject.services.TermConditionService;
import com.marketingproject.services.VerificationCodeService;
import com.marketingproject.shared.audit.CustomRevisionListener;
import com.marketingproject.shared.constants.SharedConstants;
import com.marketingproject.shared.constants.valitation.AuthValidationMessageConstants;
import com.marketingproject.shared.constants.valitation.ClientValidationMessages;
import com.marketingproject.shared.utils.AttachmentUtils;
import com.marketingproject.shared.utils.PaginationFilterUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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
    private final BucketService bucketService;
    private final TermConditionService termConditionService;
    private final AdvertisingAttachmentRepository advertisingAttachmentRepository;

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
        return buildClientResponse(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND)));
    }

    @Override
    @Transactional(readOnly = true)
    public Client findEntityById(UUID id) {
        return repository.findActiveById(id).orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponseDto getDataFromToken() {
        UUID clientId = authenticatedUserService.getLoggedUser().client().getId();
        return buildClientResponse(repository.findActiveById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND)));
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
    public void uploadAttachments(List<AttachmentRequestDto> request, UUID clientId) {
        attachmentHelper.validate(request);

        AuthenticatedUser authenticatedUser = authenticatedUserService.validateSelfOrAdmin(clientId);

        Client client = findEntityById(clientId);

        if (!client.getAttachments().isEmpty()) {
            CustomRevisionListener.setUsername(authenticatedUser.client().getBusinessName());
            client.setUsernameUpdate(authenticatedUser.client().getBusinessName());
        }

        attachmentHelper.saveAttachments(request, client, false);
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

        boolean isAdmin = authenticatedUser.client().isAdmin();

        attachmentHelper.saveAttachments(request, client, isAdmin);
        repository.save(client);
    }

    @Transactional
    @Override
    public void acceptTermsAndConditions() {
        Client client = authenticatedUserService.getLoggedUser().client();
        TermCondition actualTermCondition = termConditionService.getActualTermCondition();
        client.setTermCondition(actualTermCondition);
        client.setTermAcceptedAt(Instant.now());
        repository.save(client);
    }

    @Transactional
    @Override
    public void changeRoleToPartner(UUID clientId) throws JsonProcessingException {
        Client admin = authenticatedUserService.validateAdmin().client();
        Client partner = findEntityById(clientId);

        if (!Role.PARTNER.equals(partner.getRole())) {
            CustomRevisionListener.setUsername(admin.getBusinessName());
            CustomRevisionListener.setOldData(partner.toStringMapper());

            partner.setRole(Role.PARTNER);
            partner.setUsernameUpdate(admin.getBusinessName());
            repository.save(partner);
        }
    }

    @Transactional
    @Override
    public void validateAttachment(UUID attachmentId, AttachmentValidationType validation, RefusedAttachmentRequestDto request) throws JsonProcessingException {
        Client admin = authenticatedUserService.validateAdmin().client();
        attachmentHelper.validateAttachment(attachmentId, validation, request, admin);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponseDto<List<ClientMinResponseDto>> findAllFilters(ClientFilterRequestDto request) {
        authenticatedUserService.validateAdmin();
        Sort order = request.setOrdering();

        Pageable pageable = PaginationFilterUtil.getPageable(request, order);
        Specification<Client> filter = PaginationFilterUtil.addSpecificationFilter(
                null,
                request.getGenericFilter(),
                this::filterClients
        );

        Page<Client> page = repository.findAll(filter, pageable);
        List<ClientMinResponseDto> response = page.stream().map(ClientMinResponseDto::new).toList();
        return PaginationResponseDto.fromResult(response, (int) page.getTotalElements(), page.getTotalPages(), request.getPage());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponseDto<List<AttachmentPendingResponseDto>> findPendingAttachmentsByFilter(FilterPendingAttachmentRequestDto request) {
        authenticatedUserService.validateAdmin();
        Sort order = request.setOrdering();

        Pageable pageable = PaginationFilterUtil.getPageable(request, order);
        Specification<AdvertisingAttachment> filter = PaginationFilterUtil.addSpecificationFilter(
                (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("validation"), AttachmentValidationType.PENDING),
                request.getGenericFilter(),
                this::filterAttachments
        );

        Page<AdvertisingAttachment> page = advertisingAttachmentRepository.findAll(filter, pageable);
        List<AttachmentPendingResponseDto> response = page.stream().map(attachment -> new AttachmentPendingResponseDto(attachment, bucketService.getLink(AttachmentUtils.format(attachment)))).toList();
        return PaginationResponseDto.fromResult(response, (int) page.getTotalElements(), page.getTotalPages(), request.getPage());
    }

    Specification<Client> filterClients(Specification<Client> specification, String genericFilter) {
        String filter = "%" + genericFilter.toLowerCase() + "%";

        return specification.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(
                        criteriaBuilder.concat(
                                criteriaBuilder.concat(root.get("owner").get("firstName"), " "),
                                root.get("owner").get("lastName"))), filter),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("businessName")), filter),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("businessField")), filter),
                criteriaBuilder.like(root.get("contact").get("email"), filter),
                criteriaBuilder.like(root.get("owner").get("email"), filter),
                criteriaBuilder.equal(root.get("contact").get("phone"), genericFilter),
                criteriaBuilder.equal(root.get("owner").get("phone"), genericFilter),
                criteriaBuilder.like(root.get("identificationNumber"), filter),
                criteriaBuilder.like(root.get("owner").get("identificationNumber"), filter),
                criteriaBuilder.equal(root.get("status"), genericFilter),
                criteriaBuilder.equal(root.get("role"), genericFilter)
        ));
    }

    Specification<AdvertisingAttachment> filterAttachments(Specification<AdvertisingAttachment> specification, String genericFilter) {
        return specification.and((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")),
                    "%" + genericFilter.toLowerCase() + "%"
            ));
            predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("client").get("businessName")),
                    "%" + genericFilter.toLowerCase() + "%"
            ));
            predicates.add(criteriaBuilder.equal(
                    root.get("client").get("identificationNumber"),
                    genericFilter
            ));

            try {
                LocalDate submissionDate = LocalDate.parse(genericFilter);
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.function("date", LocalDate.class, root.get("createdAt")),
                        submissionDate
                ));
            } catch (DateTimeParseException ignored) {
            }

            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        });
    }

    ClientResponseDto buildClientResponse(Client client) {
        List<LinkResponseDto> attachmentLinks = client.getAttachments().stream()
                .map(attachment -> new LinkResponseDto(attachment.getId(), bucketService.getLink(AttachmentUtils.format(attachment))))
                .toList();

        List<LinkResponseDto> advertisingAttachmentLinks = client.getAdvertisingAttachments().stream()
                .map(attachment -> new LinkResponseDto(attachment.getId(), bucketService.getLink(AttachmentUtils.format(attachment))))
                .toList();

        return new ClientResponseDto(client, attachmentLinks, advertisingAttachmentLinks);
    }

    protected Client findActiveByIdentification(String identification) {
        return repository.findActiveByIdentificationNumber(identification)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
    }

    protected Client findByIdentification(String identification) {
        return repository.findByIdentificationNumber(identification)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
    }
}
