package com.telas.services.impl;

import com.telas.dtos.EmailDataDto;
import com.telas.dtos.request.AttachmentRequestDto;
import com.telas.dtos.request.ClientRequestDto;
import com.telas.dtos.request.PermanentDeleteClientRequestDto;
import com.telas.dtos.response.*;
import com.telas.entities.*;
import com.telas.enums.AdValidationType;
import com.telas.enums.CodeType;
import com.telas.enums.DefaultStatus;
import com.telas.enums.NotificationReference;
import com.telas.enums.Permission;
import com.telas.enums.Role;
import com.telas.services.ad.AdApprovalWorkflow;
import com.telas.services.ad.AdUploadService;
import com.telas.helpers.ClientHelper;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ForbiddenException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.infra.exceptions.UnauthorizedException;
import com.telas.infra.security.model.AuthenticatedUser;
import com.telas.infra.security.model.PasswordRequestDto;
import com.telas.infra.security.model.PasswordUpdateRequestDto;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.AdRequestRepository;
import com.telas.repositories.ClientRepository;
import com.telas.services.*;
import com.telas.shared.audit.CustomRevisionListener;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.AuthValidationMessageConstants;
import com.telas.shared.constants.valitation.ClientValidationMessages;
import com.telas.shared.utils.AttachmentUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ClientProfileServiceImpl implements ClientProfileService {

    private final ClientRepository repository;
    private final AdRequestRepository adRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClientHelper helper;
    private final AdUploadService adUploadService;
    private final AdApprovalWorkflow adApprovalWorkflow;
    private final VerificationCodeService verificationCodeService;
    private final AuthenticatedUserService authenticatedUserService;
    private final BucketService bucketService;
    private final TermConditionService termConditionService;
    private final PermissionService permissionService;
    private final AdminEmailAlertPreferenceService adminEmailAlertPreferenceService;
    private final ClientPermanentDeletionService clientPermanentDeletionService;
    private final NotificationService notificationService;
    private final BusinessQuestionnaireService businessQuestionnaireService;
    private final PartnerPlatformSettingsService partnerPlatformSettingsService;

    @Value("${front.base.url}")
    private String frontBaseUrl;

    @Override
    @Transactional
    public void save(ClientRequestDto request) {
        helper.validateClientRequest(request, null);

        Client client = new Client(request);
        helper.verifyAddressesUnique(request.getAddresses(), client);
        VerificationCode verificationCode = verificationCodeService.save(CodeType.CONTACT, client);

        client.setVerificationCode(verificationCode);

        client.setRole(Role.CLIENT);

        Client savedClient = repository.save(client);
        if (Role.ADMIN.equals(savedClient.getRole())) {
            adminEmailAlertPreferenceService.ensureDefaultEmailPreferencesForAdmin(savedClient.getId());
        }
        sendContactConfirmationEmail(savedClient, verificationCode);
        notifyAdminsNewClientRegistered(savedClient);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponseDto findById(UUID id) {
        return buildClientResponse(findEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponseDto findByEmailUnprotected(String email) {
        return repository.findByEmail(email).map(this::buildClientResponse)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Client findActiveEntityById(UUID id) {
        return repository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Client findEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponseDto getDataFromToken() {
        UUID clientId = authenticatedUserService.getLoggedUser().client().getId();
        Client client = repository.findActiveForAuthenticatedSession(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
        return buildAuthenticatedSessionResponse(client);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientWorkspaceResponseDto getClientWorkspace() {
        UUID clientId = authenticatedUserService.getLoggedUser().client().getId();
        Client client = repository.findActiveIdFromToken(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
        client.getAttachments().size();
        client.getAds().size();
        client.getAdRequests().size();
        return buildClientWorkspace(client);
    }

    @Override
    @Transactional
    public void validateCode(String email, String codigo) {
        helper.validateEmail(email);
        Client client = findByEmail(email);
        verificationCodeService.validate(client, codigo);
        repository.save(client);
    }

    @Override
    @Transactional
    public void resendCode(String email) {
        helper.validateEmail(email);
        Client client = findByEmail(email);
        VerificationCode verificationCode = verificationCodeService.save(CodeType.CONTACT, client);
        client.setVerificationCode(verificationCode);
        repository.save(client);

        Map<String, String> params = new HashMap<>();
        params.put("verificationCode", verificationCode.getCode());
        params.put("name", client.getBusinessName());
        params.put("clientId", client.getId().toString());

        EmailDataDto emailData = new EmailDataDto(client.getContact().getEmail(),
                SharedConstants.TEMPLATE_EMAIL_CONTACT_VERIFICATION, SharedConstants.EMAIL_SUBJECT_CONTACT_VERIFICATION,
                params);
        verificationCodeService.send(emailData);
    }

    @Override
    @Transactional
    public void createPassword(String email, PasswordRequestDto request) {
        request.validate();
        helper.validateEmail(email);
        Client client = findByEmail(email);

        if (!DefaultStatus.ACTIVE.equals(client.getStatus())) {
            helper.verifyValidationCode(client);
            String hashedPass = passwordEncoder.encode(request.getPassword());
            client.setPassword(hashedPass);
            client.setStatus(DefaultStatus.ACTIVE);

            repository.save(client);
        }
    }

    @Override
    @Transactional
    public void sendResetPasswordCode(String email) {
        helper.validateEmail(email);
        Client client = findActiveByEmail(email);

        VerificationCode verificationCode = verificationCodeService.save(CodeType.PASSWORD, client);
        client.setVerificationCode(verificationCode);
        repository.save(client);

        Map<String, String> params = new HashMap<>();
        params.put("verificationCode", verificationCode.getCode());
        params.put("name", client.getBusinessName());
        params.put("clientId", client.getId().toString());

        EmailDataDto emailData = new EmailDataDto(client.getContact().getEmail(),
                SharedConstants.TEMPLATE_EMAIL_RESET_PASSWORD, SharedConstants.EMAIL_SUBJECT_RESET_PASSWORD, params);
        verificationCodeService.send(emailData);
    }

    @Override
    @Transactional
    public void resetPassword(String email, PasswordRequestDto request) {
        request.validate();
        Client client = findActiveByEmail(email);

        if (!CodeType.PASSWORD.equals(client.getVerificationCode().getCodeType())) {
            throw new BusinessRuleException(AuthValidationMessageConstants.INVALID_CODE_TYPE_FOR_PASSWORD_UPDATE);
        }

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

        if (!passwordEncoder.matches(request.getCurrentPassword(), authClient.getPassword())) {
            throw new UnauthorizedException(AuthValidationMessageConstants.INVALID_CREDENTIALS);
        }

        String hashedPass = passwordEncoder.encode(request.getPassword());
        client.setPassword(hashedPass);
        repository.save(client);
    }

    @Override
    @Transactional
    public void update(ClientRequestDto request, UUID id) {
        AuthenticatedUser authenticatedUser = authenticatedUserService.validateSelfOrAdmin(id);

        Client client = findActiveEntityById(id);
        helper.validateClientRequest(request, client);

        CustomRevisionListener.setUsername(authenticatedUser.client().getBusinessName());

        client.update(request, authenticatedUser.client().getBusinessName());
        helper.updateAddresses(request.getAddresses(), client);
        repository.save(client);
    }

    @Override
    @Transactional
    public void uploadAttachments(List<AttachmentRequestDto> request) {
        adUploadService.validate(request);

        Client logged = authenticatedUserService.getLoggedUser().client();
        Client client = logged.isPartner()
                ? repository.findActiveIdFromToken(logged.getId())
                        .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND))
                : authenticatedUserService.validateActiveSubscription().client();
        boolean isFirstUpload = client.getAttachments().isEmpty();
        helper.validateAttachmentsCount(client, request);

        if (!client.getAttachments().isEmpty()) {
            CustomRevisionListener.setUsername(client.getBusinessName());
            client.setUsernameUpdate(client.getBusinessName());
        }

        adUploadService.saveAttachments(request, client);
        repository.save(client);

        if (isFirstUpload) {
            adApprovalWorkflow.notifyAdminsClientFirstAttachmentsUploaded(client);
            Map<String, String> clientAckParams = new HashMap<>();
            clientAckParams.put("name", client.getBusinessName());
            String materialsPath = client.isPartner() ? "/client/screens" : "/client/my-telas";
            clientAckParams.put("link", frontBaseUrl + materialsPath);
            clientAckParams.put("partner", client.isPartner() ? "true" : "false");
            clientAckParams.put("linkLabel", client.isPartner() ? "My screens" : "My Telas");
            notificationService.save(
                    NotificationReference.CLIENT_FIRST_ATTACHMENTS_UPLOADED_ACK,
                    client,
                    clientAckParams,
                    true
            );
        }
    }

    @Override
    @Transactional
    public void deleteClientAttachment(UUID attachmentId) {
        Client client = repository.findActiveIdFromToken(authenticatedUserService.getLoggedUser().client().getId())
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
        adUploadService.deleteClientAttachment(client, attachmentId);
    }

    @Override
    @Transactional
    public void acceptTermsAndConditions() {
        Client client = authenticatedUserService.getLoggedUser().client();
        TermCondition actualTermCondition = termConditionService.getLastTermCondition();
        client.setTermCondition(actualTermCondition);
        client.setTermAcceptedAt(Instant.now());
        repository.save(client);
    }

    @Override
    @Transactional
    public void deactivateClientByDeveloper(UUID clientId) {
        authenticatedUserService.validatePermission(Permission.ADMIN_CLIENTS_DEACTIVATE);
        Client actor = authenticatedUserService.getLoggedUser().client();
        Client target = repository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
        if (target.getId().equals(actor.getId())) {
            throw new ForbiddenException(ClientValidationMessages.CANNOT_DEACTIVATE_USER);
        }
        if (target.isAdmin() || target.isDeveloper()) {
            throw new ForbiddenException(ClientValidationMessages.CANNOT_DEACTIVATE_USER);
        }
        if (DefaultStatus.INACTIVE.equals(target.getStatus())
                || DefaultStatus.DELETED.equals(target.getStatus())) {
            return;
        }
        CustomRevisionListener.setUsername(actor.getBusinessName());
        target.setStatus(DefaultStatus.INACTIVE);
        target.setInactiveByClientId(actor.getId());
        target.setUsernameUpdate(actor.getBusinessName());
        if (target.getAdRequest() != null && target.getAdRequest().isActive()) {
            target.getAdRequest().closeRequest();
        }
        repository.save(target);
    }

    @Override
    @Transactional
    public void reactivateClientByDeveloper(UUID clientId) {
        authenticatedUserService.validatePermission(Permission.ADMIN_CLIENTS_REACTIVATE);
        Client actor = authenticatedUserService.getLoggedUser().client();
        Client target = repository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
        if (target.getId().equals(actor.getId())) {
            throw new ForbiddenException(ClientValidationMessages.CANNOT_DEACTIVATE_USER);
        }
        if (target.isAdmin() || target.isDeveloper()) {
            throw new ForbiddenException(ClientValidationMessages.CANNOT_DEACTIVATE_USER);
        }
        if (!DefaultStatus.INACTIVE.equals(target.getStatus())) {
            throw new ForbiddenException(ClientValidationMessages.CLIENT_NOT_INACTIVE);
        }
        CustomRevisionListener.setUsername(actor.getBusinessName());
        target.setStatus(DefaultStatus.ACTIVE);
        target.setInactiveByClientId(null);
        target.setUsernameUpdate(actor.getBusinessName());
        repository.save(target);
    }

    @Override
    @Transactional
    public void softDeleteClientByDeveloper(UUID clientId) {
        authenticatedUserService.validatePermission(Permission.ADMIN_CLIENTS_SOFT_DELETE);
        Client actor = authenticatedUserService.getLoggedUser().client();
        Client target = repository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
        if (target.getId().equals(actor.getId())) {
            throw new ForbiddenException(ClientValidationMessages.CANNOT_DEACTIVATE_USER);
        }
        if (target.isAdmin() || target.isDeveloper()) {
            throw new ForbiddenException(ClientValidationMessages.CANNOT_DEACTIVATE_USER);
        }
        if (DefaultStatus.DELETED.equals(target.getStatus())) {
            return;
        }
        CustomRevisionListener.setUsername(actor.getBusinessName());
        target.setStatus(DefaultStatus.DELETED);
        target.setUsernameUpdate(actor.getBusinessName());
        repository.save(target);
    }

    @Override
    @Transactional
    public void restoreSoftDeletedClientByDeveloper(UUID clientId) {
        authenticatedUserService.validatePermission(Permission.ADMIN_CLIENTS_RESTORE_DELETED);
        Client actor = authenticatedUserService.getLoggedUser().client();
        Client target = repository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
        if (target.getId().equals(actor.getId())) {
            throw new ForbiddenException(ClientValidationMessages.CANNOT_DEACTIVATE_USER);
        }
        if (target.isAdmin() || target.isDeveloper()) {
            throw new ForbiddenException(ClientValidationMessages.CANNOT_DEACTIVATE_USER);
        }
        if (!DefaultStatus.DELETED.equals(target.getStatus())) {
            throw new ForbiddenException(ClientValidationMessages.CLIENT_NOT_DELETED);
        }
        CustomRevisionListener.setUsername(actor.getBusinessName());
        target.setStatus(DefaultStatus.ACTIVE);
        target.setUsernameUpdate(actor.getBusinessName());
        repository.save(target);
    }

    @Override
    @Transactional(readOnly = true)
    public PermanentDeletionRequirementsDto getPermanentDeletionRequirements(UUID clientId) {
        validatePermanentDeleteAccess();
        return clientPermanentDeletionService.getRequirements(clientId);
    }

    @Override
    @Transactional
    public void permanentlyDeleteClientByDeveloper(UUID clientId, PermanentDeleteClientRequestDto request) {
        validatePermanentDeleteAccess();
        AuthenticatedUser logged = authenticatedUserService.getLoggedUser();
        if (!passwordEncoder.matches(request.getPassword(), logged.getPassword())) {
            throw new BusinessRuleException(AuthValidationMessageConstants.INVALID_CREDENTIALS);
        }
        Client actor = logged.client();
        Client target = repository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
        if (target.getId().equals(actor.getId())) {
            throw new ForbiddenException(ClientValidationMessages.CANNOT_DEACTIVATE_USER);
        }
        if (target.isAdmin() || target.isDeveloper()) {
            throw new ForbiddenException(ClientValidationMessages.CANNOT_DEACTIVATE_USER);
        }
        clientPermanentDeletionService.deleteClientAndOwnedData(clientId, request.getMonitorSuccessorClientId());
    }

    @Override
    @Transactional
    public void incrementSubscriptionFlow() {
        Client client = authenticatedUserService.getLoggedUser().client();
        client.getSubscriptionFlow().nextStep();
        repository.save(client);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponseDto buildClientResponse(Client client) {
        ClientWorkspaceResponseDto workspace = buildClientWorkspace(client);
        return toClientResponse(client, workspace);
    }

    private ClientResponseDto buildAuthenticatedSessionResponse(Client client) {
        return toClientResponse(client, new ClientWorkspaceResponseDto(null, List.of(), List.of()));
    }

    private ClientResponseDto toClientResponse(Client client, ClientWorkspaceResponseDto workspace) {
        boolean partnerSlotsAnyLocationEnabled =
                Role.PARTNER.equals(client.getRole()) && partnerPlatformSettingsService.isSlotsAnyLocationEnabled();
        boolean adminCanCreatePartnerEnabled = resolveAdminCanCreatePartnerEnabled(client);
        boolean hasAdRequest = adRequestRepository.existsByClientId(client.getId());

        return new ClientResponseDto(
                client,
                workspace.getAttachments(),
                workspace.getAds(),
                permissionService.listEffectivePermissionCodesForDisplay(client),
                partnerSlotsAnyLocationEnabled,
                adminCanCreatePartnerEnabled,
                workspace.getAdRequest(),
                hasAdRequest);
    }

    private ClientWorkspaceResponseDto buildClientWorkspace(Client client) {
        List<LinkResponseDto> attachmentLinks = client.getAttachments().stream()
                .filter(attachment -> !attachment.isReferenceConsumed())
                .map(attachment -> new LinkResponseDto(
                        attachment.getId(),
                        attachment.getName(),
                        bucketService.getLink(AttachmentUtils.format(attachment)),
                        bucketService.getDownloadLink(AttachmentUtils.format(attachment), attachment.getName())))
                .toList();

        List<AdResponseDto> ads = client.getAds().stream()
                .filter(ad -> !AdValidationType.REJECTED.equals(ad.getValidation()))
                .map(ad -> new AdResponseDto(
                        ad,
                        adUploadService.getStringLinkFromAd(ad),
                        adUploadService.getDownloadLinkFromAd(ad)))
                .toList();

        AdRequestClientResponseDto adRequestDto = null;
        AdRequest activeAdRequest = client.getAdRequest();
        if (activeAdRequest != null) {
            var qa = businessQuestionnaireService
                    .getLatestAnswersForClientAdRequest(client.getId(), activeAdRequest.getId())
                    .orElse(null);
            var ver = businessQuestionnaireService.findLatestVersionByAdRequestId(activeAdRequest.getId()).orElse(null);
            var updatedAt = businessQuestionnaireService.findLatestRevisionCreatedAt(activeAdRequest.getId()).orElse(null);
            adRequestDto = new AdRequestClientResponseDto(activeAdRequest, qa, ver, updatedAt);
        }

        return new ClientWorkspaceResponseDto(adRequestDto, attachmentLinks, ads);
    }

    private void notifyAdminsNewClientRegistered(Client client) {
        if (client == null || !Role.CLIENT.equals(client.getRole())) {
            return;
        }
        String contactEmail = "";
        if (client.getContact() != null && client.getContact().getEmail() != null) {
            contactEmail = client.getContact().getEmail();
        }
        String adminLink = frontBaseUrl + "/admin/clients/" + client.getId();
        Map<String, String> params = new HashMap<>();
        params.put("businessName", client.getBusinessName() != null ? client.getBusinessName() : "");
        params.put("contactEmail", contactEmail);
        params.put("clientId", client.getId().toString());
        params.put("link", adminLink);
        repository.findAllAdmins().forEach(admin ->
                notificationService.save(NotificationReference.ADMIN_NEW_CLIENT_REGISTERED, admin, new HashMap<>(params), true));
    }

    private void sendContactConfirmationEmail(Client client, VerificationCode verificationCode) {
        Map<String, String> params = new HashMap<>();
        params.put("name", client.getBusinessName());
        params.put("verificationCode", verificationCode.getCode());
        params.put("clientId", client.getId().toString());

        EmailDataDto emailData = new EmailDataDto(client.getContact().getEmail(),
                SharedConstants.TEMPLATE_EMAIL_CONTACT_VERIFICATION, SharedConstants.EMAIL_SUBJECT_CONTACT_VERIFICATION,
                params);
        verificationCodeService.send(emailData);
    }

    private boolean resolveAdminCanCreatePartnerEnabled(Client client) {
        if (!client.isPrivilegedPanelUser()) {
            return false;
        }
        if (client.isDeveloper()) {
            return true;
        }
        return partnerPlatformSettingsService.isAdminCanCreatePartnerEnabled();
    }

    private Client findActiveByEmail(String email) {
        return repository.findByEmail(email).filter(client -> DefaultStatus.ACTIVE.equals(client.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
    }

    private Client findByEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
    }

    private void validatePermanentDeleteAccess() {
        Client actor = authenticatedUserService.getLoggedUser().client();
        if (actor.isDeveloper()) {
            return;
        }
        if (permissionService.hasPermission(actor, Permission.ADMIN_CLIENTS_PERMANENT_DELETE)
                || permissionService.hasPermission(actor, Permission.ADMIN_CLIENTS_SOFT_DELETE)) {
            return;
        }
        throw new ForbiddenException(AuthValidationMessageConstants.ERROR_NO_PERMISSION);
    }
}
