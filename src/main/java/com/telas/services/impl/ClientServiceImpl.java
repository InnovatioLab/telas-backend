package com.telas.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telas.dtos.EmailDataDto;
import com.telas.dtos.request.*;
import com.telas.dtos.request.filters.ClientFilterRequestDto;
import com.telas.dtos.request.filters.FilterAdRequestDto;
import com.telas.dtos.response.*;
import com.telas.entities.*;
import com.telas.enums.AdValidationType;
import com.telas.enums.CodeType;
import com.telas.enums.DefaultStatus;
import com.telas.enums.Role;
import com.telas.helpers.AttachmentHelper;
import com.telas.helpers.ClientRequestHelper;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.infra.security.model.AuthenticatedUser;
import com.telas.infra.security.model.PasswordRequestDto;
import com.telas.infra.security.model.PasswordUpdateRequestDto;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.AdRequestRepository;
import com.telas.repositories.ClientRepository;
import com.telas.services.BucketService;
import com.telas.services.ClientService;
import com.telas.services.TermConditionService;
import com.telas.services.VerificationCodeService;
import com.telas.shared.audit.CustomRevisionListener;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.AuthValidationMessageConstants;
import com.telas.shared.constants.valitation.ClientValidationMessages;
import com.telas.shared.utils.AttachmentUtils;
import com.telas.shared.utils.PaginationFilterUtil;
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
  private final ClientRequestHelper helper;
  private final AttachmentHelper attachmentHelper;
  private final VerificationCodeService verificationCodeService;
  private final AuthenticatedUserService authenticatedUserService;
  private final BucketService bucketService;
  private final TermConditionService termConditionService;
  private final AdRequestRepository adRequestRepository;

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

    Client client = findActiveEntityById(id);
    helper.validateClientRequest(request, client);

    CustomRevisionListener.setUsername(authenticatedUser.client().getBusinessName());
    CustomRevisionListener.setOldData(client.toStringMapper());

    client.update(request, authenticatedUser.client().getBusinessName());
    helper.updateAddresses(request.getAddresses(), client);
    repository.save(client);
  }

  @Transactional
  @Override
  public void uploadAttachments(List<AttachmentRequestDto> request, UUID clientId) {
    attachmentHelper.validate(request);

    AuthenticatedUser authenticatedUser = authenticatedUserService.validateSelfOrAdmin(clientId);

    Client client = findActiveEntityById(clientId);

//        Validar se o client possui algum plano ativou ou se é um partner

    if (!client.getAttachments().isEmpty()) {
      CustomRevisionListener.setUsername(authenticatedUser.client().getBusinessName());
      client.setUsernameUpdate(authenticatedUser.client().getBusinessName());
    }

    attachmentHelper.saveAttachments(request, client, null);
    repository.save(client);
  }

  @Transactional
  @Override
  public void requestAdCreation(ClientAdRequestToAdminDto request) {
    //        Validar se o client possui algum plano ativou ou se é um partner
    Client client = authenticatedUserService.getLoggedUser().client();
    helper.createAdRequest(request, client);
  }

  @Transactional
  @Override
  public void uploadAds(AdRequestDto request) {
    //        Validar se o client possui algum plano ativou ou se é um partner
    request.validate();

    authenticatedUserService.validateAdmin();
    AdRequest adRequest = helper.getAdRequestById(request.getAdRequestId());
    Client client = adRequest.getClient();

    if (!adRequest.isActive()) {
      throw new BusinessRuleException(ClientValidationMessages.AD_REQUEST_NOT_ACTIVE);
    }

    attachmentHelper.saveAttachments(List.of(request), client, adRequest);
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
    Client partner = repository.findById(clientId).orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));

    if (!Role.PARTNER.equals(partner.getRole())) {
      CustomRevisionListener.setUsername(admin.getBusinessName());
      CustomRevisionListener.setOldData(partner.toStringMapper());

      partner.setRole(Role.PARTNER);
      partner.setUsernameUpdate(admin.getBusinessName());
      repository.save(partner);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<AdResponseDto> findPendingAds() {
    return authenticatedUserService.getLoggedUser().client().getPendingAds().stream()
            .map(ad -> new AdResponseDto(ad, attachmentHelper.getStringLinkFromAd(ad)))
            .toList();
  }

  @Override
  @Transactional
  public void validateAd(UUID adId, AdValidationType validation, RefusedAdRequestDto request) throws JsonProcessingException {
    Ad ad = helper.getAdById(adId);
    Client client = authenticatedUserService.validateSelfOrAdmin(ad.getClient().getId()).client();

    attachmentHelper.validateAd(ad, validation, request, client);
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
  public PaginationResponseDto<List<AdRequestResponseDto>> findPendingAdRequest(FilterAdRequestDto request) {
    authenticatedUserService.validateAdmin();
    Sort order = request.setOrdering();

    Pageable pageable = PaginationFilterUtil.getPageable(request, order);
    Specification<AdRequest> filter = PaginationFilterUtil.addSpecificationFilter(
            (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("isActive"), true),
            request.getGenericFilter(),
            this::filterAdRequests
    );

    Page<AdRequest> page = adRequestRepository.findAll(filter, pageable);
    List<AdRequestResponseDto> response = page.stream().map(adRequest -> new AdRequestResponseDto(adRequest, attachmentHelper.getLinksResponseFromAdRequest(adRequest))).toList();
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

  Specification<AdRequest> filterAdRequests(Specification<AdRequest> specification, String genericFilter) {
    return specification.and((root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      predicates.add(criteriaBuilder.like(
              criteriaBuilder.lower(root.get("client").get("businessName")),
              "%" + genericFilter.toLowerCase() + "%"
      ));
      predicates.add(criteriaBuilder.equal(
              root.get("client").get("identificationNumber"), genericFilter
      ));
      predicates.add(criteriaBuilder.equal(
              root.get("client").get("contact").get("email"), genericFilter
      ));
      predicates.add(criteriaBuilder.equal(
              root.get("client").get("contact").get("phone"), genericFilter
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
            .map(attachment -> new LinkResponseDto(attachment.getId(), bucketService.getLink(AttachmentUtils.format(attachment))))
            .toList();

    List<LinkResponseDto> advertisingAttachmentLinks = client.getAds().stream()
            .map(attachment -> new LinkResponseDto(attachment.getId(), bucketService.getLink(AttachmentUtils.format(attachment))))
            .toList();

    ClientResponseDto response = new ClientResponseDto(client, attachmentLinks, advertisingAttachmentLinks);
    helper.setCartResponse(response);
    return response;
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
