package com.telas.services.impl;

import com.telas.dtos.EmailDataDto;
import com.telas.dtos.request.AttachmentRequestDto;
import com.telas.dtos.request.ClientAdRequestToAdminDto;
import com.telas.dtos.request.ClientRequestDto;
import com.telas.dtos.request.RefusedAdRequestDto;
import com.telas.dtos.request.filters.ClientFilterRequestDto;
import com.telas.dtos.request.filters.FilterAdRequestDto;
import com.telas.dtos.response.*;
import com.telas.entities.*;
import com.telas.enums.AdValidationType;
import com.telas.enums.CodeType;
import com.telas.enums.DefaultStatus;
import com.telas.enums.Role;
import com.telas.helpers.AttachmentHelper;
import com.telas.helpers.ClientHelper;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ForbiddenException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.infra.exceptions.UnauthorizedException;
import com.telas.infra.model.AuthenticatedUser;
import com.telas.infra.model.PasswordRequestDto;
import com.telas.infra.model.PasswordUpdateRequestDto;
import com.telas.repositories.AdRequestRepository;
import com.telas.repositories.ClientRepository;
import com.telas.services.BucketService;
import com.telas.services.ClientService;
import com.telas.services.TermConditionService;
import com.telas.services.VerificationCodeService;
import com.telas.shared.audit.CustomRevisionListener;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.AdValidationMessages;
import com.telas.shared.constants.valitation.AuthValidationMessageConstants;
import com.telas.shared.constants.valitation.ClientValidationMessages;
import com.telas.shared.utils.AttachmentUtils;
import com.telas.shared.utils.PaginationFilterUtil;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
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
import java.util.*;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {
    private final ClientRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final ClientHelper helper;
    private final AttachmentHelper attachmentHelper;
    private final VerificationCodeService verificationCodeService;

    private final BucketService bucketService;
    private final TermConditionService termConditionService;
    private final AdRequestRepository adRequestRepository;

    @Override
    @Transactional
    public void save(ClientRequestDto request) {
        helper.validateClientRequest(request, null);

        Client client = new Client(request);
        VerificationCode verificationCode = verificationCodeService.save(CodeType.CONTACT, client);
        client.setVerificationCode(verificationCode);

        if (Objects.equals(client.getBusinessName(), "Admin")) {
            client.setRole(Role.ADMIN);
            TermCondition actualTermCondition = termConditionService.getLastTermCondition();
            client.setTermCondition(actualTermCondition);
            client.setTermAcceptedAt(Instant.now());
        } else {
            client.setRole(Role.CLIENT);
        }

        sendContactConfirmationEmail(client, verificationCode);
        repository.save(client);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponseDto findById(UUID id) {
        return buildClientResponse(findEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponseDto findByEmailUnprotected(String email) {
        return repository.findByEmail(email)
                .map(this::buildClientResponse)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Client findActiveEntityById(UUID id) {
        return repository.findActiveById(id).orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Client findEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponseDto getDataFromToken(UUID clientId) {
        UUID clientId = authenticatedUserService.getLoggedUser().client().getId();
        return buildClientResponse(repository.findActiveIdFromToken(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND)));
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

        EmailDataDto emailData = new EmailDataDto(client.getContact().getEmail(), SharedConstants.TEMPLATE_EMAIL_CONTACT_VERIFICATION, SharedConstants.EMAIL_SUBJECT_CONTACT_VERIFICATION, params);
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
    @Transactional(readOnly = true)
    public void sendResetPasswordCode(String email) {
        helper.validateEmail(email);
        Client client = findActiveByEmail(email);

        VerificationCode verificationCode = verificationCodeService.save(CodeType.PASSWORD, client);
        client.setVerificationCode(verificationCode);
        repository.save(client);

        Map<String, String> params = new HashMap<>();
        params.put("verificationCode", verificationCode.getCode());
        params.put("name", client.getBusinessName());

        EmailDataDto emailData = new EmailDataDto(client.getContact().getEmail(), SharedConstants.TEMPLATE_EMAIL_RESET_PASSWORD, SharedConstants.EMAIL_SUBJECT_RESET_PASSWORD, params);
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

    @Transactional
    @Override
    public void update(ClientRequestDto request, UUID id) {
        AuthenticatedUser authenticatedUser = authenticatedUserService.validateSelfOrAdmin(id);

        Client client = findActiveEntityById(id);
        helper.validateClientRequest(request, client);

        CustomRevisionListener.setUsername(authenticatedUser.client().getBusinessName());

        client.update(request, authenticatedUser.client().getBusinessName());
        helper.updateAddresses(request.getAddresses(), client);
        repository.save(client);
    }

    @Transactional
    @Override
    public void uploadAttachments(List<AttachmentRequestDto> request) {
        attachmentHelper.validate(request);

        Client client = authenticatedUserService.validateActiveSubscription().client();
        helper.validateAttachmentsCount(client, request);

        if (!client.getAttachments().isEmpty()) {
            CustomRevisionListener.setUsername(client.getBusinessName());
            client.setUsernameUpdate(client.getBusinessName());
        }

        attachmentHelper.saveAttachments(request, client);
        repository.save(client);
    }

    @Transactional
    @Override
    public void requestAdCreation(ClientAdRequestToAdminDto request) {
        Client client = authenticatedUserService.validateActiveSubscription().client();

        if (Role.ADMIN.equals(client.getRole())) {
            return;
        }

        if (Objects.nonNull(client.getAdRequest())) {
            throw new ForbiddenException(ClientValidationMessages.AD_REQUEST_EXISTS);
        }

        if (!client.getAds().isEmpty()) {
            throw new BusinessRuleException(ClientValidationMessages.MAX_ADS_REACHED);
        }

        helper.createAdRequest(request, client);
    }

    @Transactional
    @Override
    public void uploadAds(AttachmentRequestDto request, UUID clientId) {
        request.validate();

        Client admin = authenticatedUserService.validateAdmin().client();

        if (admin.getId().equals(clientId)) {
            attachmentHelper.saveAds(request, admin);
            return;
        }

        Client client = findActiveEntityById(clientId);

        if (Objects.isNull(client.getAdRequest())) {
            throw new ResourceNotFoundException(ClientValidationMessages.AD_REQUEST_NOT_FOUND);
        }

        attachmentHelper.saveAds(request, client);
    }

    @Transactional
    @Override
    public void acceptTermsAndConditions() {
        Client client = authenticatedUserService.getLoggedUser().client();
        TermCondition actualTermCondition = termConditionService.getLastTermCondition();
        client.setTermCondition(actualTermCondition);
        client.setTermAcceptedAt(Instant.now());
        repository.save(client);
    }

    @Transactional
    @Override
    public void changeRoleToPartner(UUID clientId) {
        Client admin = authenticatedUserService.validateAdmin().client();
        Client partner = repository.findById(clientId).orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));

        if (!Role.PARTNER.equals(partner.getRole())) {
            CustomRevisionListener.setUsername(admin.getBusinessName());

            partner.setRole(Role.PARTNER);
            partner.setUsernameUpdate(admin.getBusinessName());
            repository.save(partner);
        }
    }

    @Override
    @Transactional
    public void validateAd(UUID adId, AdValidationType validation, RefusedAdRequestDto request) {
        Ad ad = helper.getAdById(adId);
        attachmentHelper.validateAd(ad, validation, request);

        if (AdValidationType.APPROVED.equals(validation)) {
            List<UUID> monitorIds = helper.findClientMonitorsWithActiveSubscriptions(ad.getClient().getId()).stream()
                    .map(Monitor::getId)
                    .toList();

            if (!monitorIds.isEmpty()) {
                helper.addAdToMonitor(ad, monitorIds, ad.getClient());
            }
        }
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
    public PaginationResponseDto<List<ClientMinResponseDto>> findAllFilters(ClientFilterRequestDto request) {
        authenticatedUserService.validateAdmin();
        Sort order = request.setOrdering();

        Pageable pageable = PaginationFilterUtil.getPageable(request, order);
        Specification<Client> filter = PaginationFilterUtil.addSpecificationFilter(
                (root, query, criteriaBuilder) -> criteriaBuilder.notEqual(root.get("role"), Role.ADMIN),
                request.getGenericFilter(),
                this::filterClients
        );

        Page<Client> page = repository.findAll(filter, pageable);
        List<ClientMinResponseDto> response = page.stream().map(ClientMinResponseDto::new).toList();
        return PaginationResponseDto.fromResult(response, (int) page.getTotalElements(), page.getTotalPages(), request.getPage());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponseDto<List<AdRequestAdminResponseDto>> findPendingAdRequest(FilterAdRequestDto request) {
        authenticatedUserService.validateAdmin();
        Sort order = request.setOrdering();

        Pageable pageable = PaginationFilterUtil.getPageable(request, order);
        Specification<AdRequest> filter = PaginationFilterUtil.addSpecificationFilter(
                (root, query, criteriaBuilder) -> {
                    query.distinct(true);

                    Join<AdRequest, Ad> adJoin = root.join("ad", JoinType.LEFT);
                    Join<Ad, RefusedAd> refusedAdsJoin = adJoin.join("refusedAds", JoinType.LEFT);

                    query.groupBy(root.get("id"));

                    Expression<Long> refusedCount = criteriaBuilder.count(refusedAdsJoin.get("id"));
                    Predicate isActive = criteriaBuilder.equal(root.get("isActive"), true);

                    query.having(criteriaBuilder.le(refusedCount, (long) SharedConstants.MAX_ADS_VALIDATION));
                    return criteriaBuilder.and(isActive);
                },
                request.getGenericFilter(),
                this::filterAdRequests
        );

        Page<AdRequest> page = adRequestRepository.findAll(filter, pageable);
        List<AdRequestAdminResponseDto> response = page.stream().map(adRequest -> new AdRequestAdminResponseDto(adRequest, attachmentHelper.getAdRequestData(adRequest))).toList();
        return PaginationResponseDto.fromResult(response, (int) page.getTotalElements(), page.getTotalPages(), request.getPage());
    }

    @Override
    @Transactional
    public void addMonitorToWishlist(UUID monitorId) {
        Client client = authenticatedUserService.getLoggedUser().client();
        helper.addMonitorToWishlist(monitorId, client);
    }

    @Override
    @Transactional(readOnly = true)
    public WishlistResponseDto getWishlistMonitors() {
        Client client = authenticatedUserService.getLoggedUser().client();

        if (client.getWishlist() == null) {
            throw new ResourceNotFoundException(ClientValidationMessages.WISHLIST_NOT_FOUND);
        }

        return new WishlistResponseDto(client.getWishlist());
    }

    private void validateAdRequestId(UUID adRequestId) {
        if (adRequestId == null) {
            throw new BusinessRuleException(AdValidationMessages.AD_REQUEST_ID_REQUIRED);
        }
    }

    private void sendContactConfirmationEmail(Client client, VerificationCode verificationCode) {
        Map<String, String> params = new HashMap<>();
        params.put("name", client.getBusinessName());
        params.put("verificationCode", verificationCode.getCode());

        EmailDataDto emailData = new EmailDataDto(client.getContact().getEmail(), SharedConstants.TEMPLATE_EMAIL_CONTACT_VERIFICATION, SharedConstants.EMAIL_SUBJECT_CONTACT_VERIFICATION, params);
        verificationCodeService.send(emailData);
    }

    Specification<Client> filterClients(Specification<Client> specification, String genericFilter) {
        String filter = "%" + genericFilter.toLowerCase() + "%";

        return specification.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("businessName")), filter),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("industry")), filter),
                criteriaBuilder.like(root.get("contact").get("email"), filter),
                criteriaBuilder.equal(root.get("contact").get("phone"), genericFilter),
                criteriaBuilder.equal(root.get("status"), genericFilter),
                criteriaBuilder.equal(root.get("role"), genericFilter)
        ));
    }

    Specification<AdRequest> filterAdRequests(Specification<AdRequest> specification, String genericFilter) {
        return specification.and((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            String filter = "%" + genericFilter.toLowerCase() + "%";

            predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("client").get("businessName")),
                    filter
            ));
            predicates.add(criteriaBuilder.like(
                    root.get("client").get("contact").get("email"), filter
            ));
            predicates.add(criteriaBuilder.like(
                    root.get("client").get("contact").get("phone"), filter
            ));
            predicates.add(criteriaBuilder.equal(
                    root.get("client").get("role"), genericFilter
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
                .map(attachment -> new LinkResponseDto(attachment.getId(), attachment.getName(), bucketService.getLink(AttachmentUtils.format(attachment))))
                .toList();

        List<AdResponseDto> ads = client.getAds().stream()
                .map(ad -> new AdResponseDto(ad, attachmentHelper.getStringLinkFromAd(ad)))
                .toList();

        return new ClientResponseDto(client, attachmentLinks, ads);
    }

    protected Client findActiveByEmail(String email) {
        return repository.findByEmail(email)
                .filter(client -> DefaultStatus.ACTIVE.equals(client.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
    }

    protected Client findByEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
    }
}
