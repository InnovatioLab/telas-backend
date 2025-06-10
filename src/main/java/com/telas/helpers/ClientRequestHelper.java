package com.telas.helpers;

import com.telas.dtos.request.AddressRequestDto;
import com.telas.dtos.request.AttachmentRequestDto;
import com.telas.dtos.request.ClientAdRequestToAdminDto;
import com.telas.dtos.request.ClientRequestDto;
import com.telas.entities.*;
import com.telas.enums.AdValidationType;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ForbiddenException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.*;
import com.telas.services.AddressService;
import com.telas.services.GeolocationService;
import com.telas.services.MonitorService;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.*;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClientRequestHelper {
  private final OwnerRepository ownerRepository;
  private final ClientRepository clientRepository;
  private final MonitorRepository monitorRepository;
  private final MonitorService monitorService;
  private final ContactRepository contactRepository;
  private final AttachmentRepository attachmentRepository;
  private final AdRepository adRepository;
  private final AdRequestRepository adRequestRepository;
  private final AddressService addressService;
  private final GeolocationService geolocationService;

  @Transactional(readOnly = true)
  public void validateClientRequest(ClientRequestDto request, Client client) {
    request.validate();
    verifyUniqueIdentificationNumber(request, client);
    verifyUniqueEmail(request, client);
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

    if (attachments.stream().anyMatch(attachment -> attachment.getClient() != null && !attachment.getClient().getId().equals(client.getId()))) {
      throw new BusinessRuleException(AttachmentValidationMessages.ATTACHMENTS_NOT_BELONG_TO_CLIENT);
    }

    AdRequest adRequest = new AdRequest(request, client, attachments);
    adRequestRepository.save(adRequest);
  }

  @Transactional(readOnly = true)
  public AdRequest getAdRequestById(UUID adRequestId) {
    return adRequestRepository.findById(adRequestId)
            .orElseThrow(() -> new ResourceNotFoundException(AdValidationMessages.AD_REQUEST_NOT_FOUND));
  }


  List<Attachment> getAttachmentsByIds(List<UUID> attachmentsIds) {
    return attachmentRepository.findByIdIn(attachmentsIds).orElseThrow(() -> new ResourceNotFoundException(AttachmentValidationMessages.ATTACHMENTS_NOT_FOUND));
  }

  void verifyUniqueEmail(ClientRequestDto request, Client client) {
    String newEmail = request.getContact().getEmail();
    String ownerEmail = request.getOwner().getEmail();

    if (client != null && isEmailChanged(client.getOwner().getEmail(), ownerEmail) && ownerRepository.existsByEmail(ownerEmail)) {
      throw new BusinessRuleException(ClientValidationMessages.EMAIL_UNIQUE);
    }

    if (client == null && (ownerRepository.existsByEmail(ownerEmail) || contactRepository.existsByEmail(newEmail))) {
      throw new BusinessRuleException(ClientValidationMessages.EMAIL_UNIQUE);
    }
  }

  void verifyUniqueIdentificationNumber(ClientRequestDto request, Client client) {
    String newIdNumber = request.getIdentificationNumber();
    String ownerIdNumber = request.getOwner().getIdentificationNumber();

    if (client != null && isIdNumberChanged(client.getOwner().getIdentificationNumber(), ownerIdNumber) && ownerRepository.existsByIdentificationNumber(ownerIdNumber)) {
      throw new BusinessRuleException(ClientValidationMessages.EMAIL_UNIQUE);
    }

    if (client == null && (ownerRepository.existsByIdentificationNumber(ownerIdNumber) || clientRepository.existsByIdentificationNumber(newIdNumber))) {
      throw new BusinessRuleException(ClientValidationMessages.IDENTIFICATION_NUMBER_UNIQUE);
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
    return adRepository.findById(adId).orElseThrow(() -> new ResourceNotFoundException(AdValidationMessages.AD_NOT_FOUND));
  }

  @Transactional
  public void updateAddresses(List<AddressRequestDto> requestList, Client client) {
    for (AddressRequestDto addressRequest : requestList) {
      Address address = addressRequest.getId() == null
              ? createNewAddress(addressRequest, client)
              : updateExistingAddress(addressRequest, client);

      addressService.save(address);
    }
  }

  private Address createNewAddress(AddressRequestDto addressRequest, Client client) {
    Address address = new Address(addressRequest, client);
    address.setUsernameCreate(client.getBusinessName());
    client.getAddresses().add(address);
    return address;
  }

  private Address updateExistingAddress(AddressRequestDto addressRequest, Client client) {
    Address address = addressService.findById(addressRequest.getId());

    if (address.hasChanged(addressRequest)) {
      BeanUtils.copyProperties(addressRequest, address, "latitude", "longitude", "client", "monitors");
      address.setUsernameUpdate(client.getBusinessName());

      if (address.isPartnerAddress()) {
        geolocationService.getAddressCoordinates(address);
      }
    }
    return address;
  }

  @Transactional(readOnly = true)
  public void validateActiveSubscription(Client client) {
    if (!client.hasActiveSubscription()) {
      throw new ForbiddenException(AuthValidationMessageConstants.ERROR_NO_ACTIVE_SUBSCRIPTION);
    }
  }

  @Transactional
  public void addAdToMonitor(Ad ad, List<UUID> monitorIds, Client client) {
    validateAd(ad);

    List<Monitor> monitors = monitorRepository.findAllById(monitorIds);

    if (monitors.size() != monitorIds.size()) {
      throw new BusinessRuleException(MonitorValidationMessages.MONITOR_NOT_FOUND);
    }

    monitors.forEach(monitor -> {
      validateMonitorAccess(client, monitor);

      MonitorAd monitorAd = new MonitorAd(monitor, ad);
      monitor.getMonitorAds().add(monitorAd);
      monitor.getClients().add(ad.getClient());
    });

    monitorRepository.saveAll(monitors);
  }

  private void validateAd(Ad ad) {
    if (ad == null) {
      throw new BusinessRuleException(AdValidationMessages.AD_NOT_FOUND);
    }

    if (!AdValidationType.APPROVED.equals(ad.getValidation())) {
      throw new BusinessRuleException(ClientValidationMessages.AD_NOT_APPROVED);
    }
  }

  private void validateMonitorAccess(Client client, Monitor monitor) {
    if (!client.getMonitorsWithActiveSubscriptions().contains(monitor)) {
      throw new ForbiddenException(ClientValidationMessages.MONITOR_WITHOUT_ACTIVE_SUBSCRIPTION);
    }

    if (monitor.clientAlreadyHasAd(client)) {
      throw new BusinessRuleException(ClientValidationMessages.CLIENT_ALREADY_HAS_AD_IN_MONITOR);
    }

    if (!monitor.hasAvailableBlocks(1)) {
      throw new BusinessRuleException(ClientValidationMessages.MONITOR_MAX_ADS_REACHED);
    }
  }

  @Transactional(readOnly = true)
  public void validateAttachmentsCount(Client client, List<AttachmentRequestDto> request) {
    int attachmentToAdd = request.stream().filter(r -> r.getId() == null).toList().size();
    int attachmentCount = client.getAttachments().size() + attachmentToAdd;

    if (attachmentCount >= SharedConstants.MAX_ATTACHMENT_PER_CLIENT) {
      throw new BusinessRuleException(AttachmentValidationMessages.MAX_ATTACHMENTS_REACHED);
    }
  }
}
