package com.telas.services.impl;

import com.telas.dtos.EmailDataDto;
import com.telas.dtos.request.*;
import com.telas.dtos.request.filters.AdminFilterAdRequestDto;
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
import com.telas.infra.security.model.AuthenticatedUser;
import com.telas.infra.security.model.PasswordRequestDto;
import com.telas.infra.security.model.PasswordUpdateRequestDto;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.AdRepository;
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
  private final AuthenticatedUserService authenticatedUserService;
  private final BucketService bucketService;
  private final TermConditionService termConditionService;
  private final AdRequestRepository adRequestRepository;
  private final AdRepository adRepository;

  @Override
  @Transactional
  public void save(ClientRequestDto request) {
    helper.validateClientRequest(request, null);
    Owner owner = helper.getOrCreateOwner(request.getOwner());

    Client client = new Client(request, owner);
    VerificationCode verificationCode = verificationCodeService.save(CodeType.CONTACT, client);
    verificationCode.setValidated(true);
    client.setVerificationCode(verificationCode);

    if (Objects.equals(client.getBusinessName(), "Admin")) {
      client.setRole(Role.ADMIN);
    } else {
      client.setRole(Role.CLIENT);
    }

    TermCondition actualTermCondition = termConditionService.getActualTermCondition();
    client.setTermCondition(actualTermCondition);
    client.setTermAcceptedAt(Instant.now());

//    sendContactConfirmationEmail(client, verificationCode);
    repository.save(client);
  }

  @Override
  @Transactional(readOnly = true)
  public ClientResponseDto findById(UUID id) {
    return buildClientResponse(findEntityById(id));
  }

  @Override
  @Transactional(readOnly = true)
  public ClientResponseDto findByIdentificationNumber(String identification) {
    return repository.findByIdentificationNumber(identification)
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
  public ClientResponseDto getDataFromToken() {
    UUID clientId = authenticatedUserService.getLoggedUser().client().getId();
    return buildClientResponse(repository.findActiveIdFromToken(clientId)
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

    Map<String, String> params = new HashMap<>();
    params.put("verificationCode", verificationCode.getCode());
    params.put("name", client.getBusinessName());

    EmailDataDto emailData = new EmailDataDto(client.getContact().getEmail(), SharedConstants.TEMPLATE_EMAIL_CONTACT_VERIFICATION, SharedConstants.EMAIL_SUBJECT_CONTACT_VERIFICATION, params);
    verificationCodeService.send(emailData);
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

    Map<String, String> params = new HashMap<>();
    params.put("verificationCode", verificationCode.getCode());
    params.put("name", client.getBusinessName());

    EmailDataDto emailData = new EmailDataDto(client.getContact().getEmail(), SharedConstants.TEMPLATE_EMAIL_RESET_PASSWORD, SharedConstants.EMAIL_SUBJECT_RESET_PASSWORD, params);
    verificationCodeService.send(emailData);
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

//    Client client = authenticatedUserService.validateActiveSubscription().client();
    Client client = authenticatedUserService.getLoggedUser().client();
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
//    Client client = authenticatedUserService.validateActiveSubscription().client();
    Client client = authenticatedUserService.getLoggedUser().client();

    if (Role.ADMIN.equals(client.getRole())) {
      return;
    }

    if (Objects.nonNull(client.getAdRequest())) {
      throw new ForbiddenException(ClientValidationMessages.AD_REQUEST_EXISTS);
    }

    if (!client.getAds().isEmpty() && client.getAds().stream().noneMatch(ad -> AdValidationType.REJECTED.equals(ad.getValidation()))) {
      throw new BusinessRuleException(ClientValidationMessages.AD_REQUEST_NOT_ALLOWED);
    }

    helper.createAdRequest(request, client);
  }

  @Transactional
  @Override
  public void uploadAds(AdRequestDto request, UUID clientId) {
    request.validate();

//    Client client = authenticatedUserService.validateActiveSubscription().client();
    Client client = authenticatedUserService.getLoggedUser().client();

    if (Role.ADMIN.equals(client.getRole()) && client.getId().equals(clientId)) {
      attachmentHelper.saveAds(request, client, null);
      return;
    }

    if (!Role.ADMIN.equals(client.getRole())) {
      if (!client.getId().equals(clientId)) {
        throw new ForbiddenException(ClientValidationMessages.UNAUTHORIZED_CLIENT);
      }

      if (!client.getAds().isEmpty()) {
        throw new BusinessRuleException(ClientValidationMessages.MAX_ADS_REACHED);
      }

      if (Objects.nonNull(client.getAdRequest())) {
        throw new ForbiddenException(ClientValidationMessages.AD_REQUEST_EXISTS);
      }

      request.setAdRequestId(null);
      attachmentHelper.saveAds(request, client, null);
      return;
    }

    validateAdRequestId(request.getAdRequestId());
    AdRequest adRequest = helper.getAdRequestById(request.getAdRequestId());
    attachmentHelper.saveAds(request, client, adRequest);
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
//    Client validator = authenticatedUserService.validateActiveSubscription().client();
    Client validator = authenticatedUserService.getLoggedUser().client();
    attachmentHelper.validateAd(ad, validation, request, validator);

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

              // HAVING count(refusedAds.id) <= 3
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
  @Transactional(readOnly = true)
  public PaginationResponseDto<List<PendingAdAdminValidationResponseDto>> findPendingAds(AdminFilterAdRequestDto request) {
    authenticatedUserService.validateAdmin();

    Pageable pageable = PaginationFilterUtil.getPageable(request, request.setOrdering());
    Specification<Ad> filter = PaginationFilterUtil.addSpecificationFilter((root, query, criteriaBuilder) -> criteriaBuilder.and(
                    criteriaBuilder.isNull(root.get("adRequest")),
                    criteriaBuilder.equal(root.get("validation"), AdValidationType.PENDING),
                    criteriaBuilder.notEqual(root.get("client").get("role"), Role.ADMIN)
            ),
            request.getGenericFilter(),
            this::filterAds
    );

    Page<Ad> page = adRepository.findAll(filter, pageable);
    List<PendingAdAdminValidationResponseDto> response = page.stream()
            .map(ad -> new PendingAdAdminValidationResponseDto(ad, attachmentHelper.getStringLinkFromAd(ad)))
            .toList();

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
            criteriaBuilder.like(criteriaBuilder.lower(
                    criteriaBuilder.concat(
                            criteriaBuilder.concat(root.get("owner").get("firstName"), " "),
                            root.get("owner").get("lastName"))), filter),
            criteriaBuilder.like(criteriaBuilder.lower(root.get("businessName")), filter),
            criteriaBuilder.like(criteriaBuilder.lower(root.get("industry")), filter),
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

  Specification<AdRequest> filterAdRequests(Specification<AdRequest> specification, String genericFilter) {
    return specification.and((root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();
      String filter = "%" + genericFilter.toLowerCase() + "%";

      predicates.add(criteriaBuilder.like(
              criteriaBuilder.lower(root.get("client").get("businessName")),
              filter
      ));
      predicates.add(criteriaBuilder.like(
              root.get("client").get("identificationNumber"), filter
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

  Specification<Ad> filterAds(Specification<Ad> specification, String genericFilter) {
    return specification.and((root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();
      String filter = "%" + genericFilter.toLowerCase() + "%";

      predicates.add(criteriaBuilder.like(
              criteriaBuilder.lower(root.get("client").get("businessName")),
              filter
      ));
      predicates.add(criteriaBuilder.like(
              root.get("client").get("identificationNumber"), filter
      ));
      predicates.add(criteriaBuilder.like(
              root.get("name"), filter
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

  protected Client findActiveByIdentification(String identification) {
    return repository.findActiveByIdentificationNumber(identification)
            .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
  }

  protected Client findByIdentification(String identification) {
    return repository.findByIdentificationNumber(identification)
            .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
  }
}
