package com.telas.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telas.dtos.request.MonitorRequestDto;
import com.telas.dtos.response.MonitorAdResponseDto;
import com.telas.dtos.response.MonitorMinResponseDto;
import com.telas.dtos.response.MonitorResponseDto;
import com.telas.dtos.response.MonitorValidationResponseDto;
import com.telas.entities.*;
import com.telas.enums.AdValidationType;
import com.telas.enums.SubscriptionStatus;
import com.telas.helpers.MonitorRequestHelper;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.infra.security.model.AuthenticatedUser;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.MonitorRepository;
import com.telas.services.BucketService;
import com.telas.services.MonitorService;
import com.telas.shared.audit.CustomRevisionListener;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.AddressValidationMessages;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.shared.utils.AttachmentUtils;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonitorServiceImpl implements MonitorService {
  private final AuthenticatedUserService authenticatedUserService;
  private final MonitorRepository repository;
  //  private final MonitorAdRepository monitorAdRepository;
  private final BucketService bucketService;
  private final MonitorRequestHelper helper;

  @Override
  @Transactional
  public void save(MonitorRequestDto request, UUID monitorId) throws JsonProcessingException {
    AuthenticatedUser authenticatedUser = authenticatedUserService.validateAdmin();
    request.validate();
    Address address = helper.getOrCreateAddress(request);

    Monitor monitor;

    if (monitorId != null) {
      List<Ad> ads = !ValidateDataUtils.isNullOrEmpty(request.getAds()) ? helper.getAds(request, monitorId) : List.of();
      Set<Client> clients = getClients(ads);
      monitor = updateExistingMonitor(request, monitorId, authenticatedUser, address, clients, ads);
    } else {
      monitor = createNewMonitor(request, authenticatedUser, address);
    }

    repository.save(monitor);
  }

  @Override
  @Transactional(readOnly = true)
  public MonitorResponseDto findById(UUID monitorId) {
    authenticatedUserService.validateAdmin();

    Monitor entity = repository.findById(monitorId).orElseThrow(() -> new ResourceNotFoundException(MonitorValidationMessages.MONITOR_NOT_FOUND));

    List<MonitorAdResponseDto> advertisingAttachmentLinks = entity.getMonitorAds().stream()
            .filter(monitorAttachment -> AdValidationType.APPROVED.equals(monitorAttachment.getAd().getValidation()))
            .map(monitorAttachment -> new MonitorAdResponseDto(monitorAttachment, bucketService.getLink(AttachmentUtils.format(monitorAttachment.getAd()))))
            .toList();

    return new MonitorResponseDto(entity, advertisingAttachmentLinks);
  }

  @Override
  @Transactional
  public Monitor findEntityById(UUID monitorId) {
    return repository.findById(monitorId).orElseThrow(() -> new ResourceNotFoundException(MonitorValidationMessages.MONITOR_NOT_FOUND));
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, List<MonitorMinResponseDto>> findNearestActiveMonitors(String zipCodes, BigDecimal sizeFilter, String typeFilter, int limit) {
    Client client = authenticatedUserService.getLoggedUser().client();
    String countryCode = client.getAddresses().stream()
            .findFirst()
            .map(Address::getCountry)
            .orElse("US");

    List<String> zipCodeList = validateZipCodeList(zipCodes);

    Map<String, List<MonitorMinResponseDto>> result = new HashMap<>();

    zipCodeList.forEach(zipCode -> {
      Map<String, Double> coordinates = helper.getCoordinatesFromZipCode(zipCode, countryCode);
      double latitude = coordinates.get("latitude");
      double longitude = coordinates.get("longitude");

      List<MonitorMinResponseDto> monitors = repository.findNearestActiveMonitorsWithFilters(latitude, longitude, sizeFilter, typeFilter, limit)
              .stream()
              .map(resultRow -> new MonitorMinResponseDto(
                      resultRow[0].toString(),
                      Boolean.parseBoolean(resultRow[1].toString()), // Ativo
                      resultRow[2].toString(), // Tipo
                      Double.parseDouble(resultRow[3].toString()), // Tamanho
                      Double.parseDouble(resultRow[6].toString()), // Distância
                      Double.parseDouble(resultRow[4].toString()), // Latitude
                      Double.parseDouble(resultRow[5].toString()), // Longitude
                      Boolean.parseBoolean(resultRow[7].toString()), // hasAvailableSlots
                      resultRow[8] != null ? Instant.parse(resultRow[8].toString()) : null // estimatedSlotReleaseDate
              ))
              .toList();

      result.put(zipCode, monitors);
    });

    return result;
  }

  @Override
  @Transactional
  public List<Monitor> findAllByIds(List<UUID> monitorIds) {
    return repository.findAllByIdIn(monitorIds);
  }

  @Override
  @Transactional
  public List<MonitorValidationResponseDto> findInvalidMonitorsDuringCheckout(List<UUID> monitorIds, UUID clientId) {
    return repository.findInvalidMonitors(monitorIds, clientId);
  }

  @Override
  @Transactional
  public void removeMonitorAdsFromSubscription(Subscription subscription) {
    List<SubscriptionStatus> validStatuses = List.of(SubscriptionStatus.EXPIRED, SubscriptionStatus.CANCELLED);

    if (validStatuses.contains(subscription.getStatus())) {
      Client client = subscription.getClient();

      // Itera sobre os monitores da subscription
      subscription.getMonitors().forEach(monitor -> {
        // Filtra os anúncios que pertencem ao cliente da subscription
        Set<MonitorAd> adsToRemove = monitor.getMonitorAds().stream()
                .filter(monitorAd -> monitorAd.getAd().getClient().equals(client))
                .collect(Collectors.toSet());

        // Remove os anúncios do monitor
        monitor.getMonitorAds().removeAll(adsToRemove);
        monitor.getClients().remove(client);

        // Remove os anúncios do repositório
//        monitorAdRepository.deleteAll(adsToRemove);
        repository.save(monitor);
      });
    }
  }

  private List<String> validateZipCodeList(String zipCodes) {
    List<String> zipCodeList = Arrays.asList(zipCodes.split(","));

    if (zipCodeList.isEmpty()) {
      throw new BusinessRuleException(MonitorValidationMessages.ZIP_CODE_LIST_EMPTY);
    }

    zipCodeList.forEach(zipCode -> {
      if (!zipCode.matches(SharedConstants.REGEX_ZIP_CODE)) {
        throw new BusinessRuleException(AddressValidationMessages.ZIP_CODE_LIST_INVALID);
      }
    });
    return zipCodeList;
  }

  private Monitor createNewMonitor(MonitorRequestDto request, AuthenticatedUser authenticatedUser, Address address) {
    helper.setAddressCoordinates(address);
    Monitor monitor = new Monitor(request, address);
    monitor.setUsernameCreate(authenticatedUser.client().getBusinessName());
    return monitor;
  }

  private Monitor updateExistingMonitor(MonitorRequestDto request, UUID monitorId, AuthenticatedUser authenticatedUser, Address address, Set<Client> clients, List<Ad> ads) throws JsonProcessingException {
    Monitor monitor = findEntityById(monitorId);

    if (!monitor.getAddress().getId().equals(request.getAddressId())) {
      monitor.setAddress(address);

      if (!address.hasLocation()) {
        helper.setAddressCoordinates(address);
      }

    }

    CustomRevisionListener.setUsername(authenticatedUser.client().getBusinessName());
    CustomRevisionListener.setOldData(monitor.toStringMapper());
    updateMonitorDetails(request, monitor, clients, ads);
    monitor.setUsernameUpdate(authenticatedUser.client().getBusinessName());

    return monitor;
  }

  private void updateMonitorDetails(MonitorRequestDto request, Monitor monitor, Set<Client> clients, List<Ad> ads) {
    monitor.setType(request.getType());
    monitor.setLocationDescription(request.getLocationDescription());
    monitor.setMaxBlocks(request.getMaxBlocks());
    monitor.setSize(request.getSize());

    if (!ValidateDataUtils.isNullOrEmpty(ads)) {
//      monitorAdRepository.deleteAll(monitor.getMonitorAds());
      monitor.getMonitorAds().clear();

      ads.forEach(attachment -> monitor.getMonitorAds().add(
              new MonitorAd(request.getAds().get(ads.indexOf(attachment)), monitor, attachment)
      ));

//      monitorAdRepository.saveAll(monitor.getMonitorAds());
    }

    monitor.getClients().clear();
    monitor.getClients().addAll(clients);
  }

  private Set<Client> getClients(List<Ad> ads) {
    return ads.isEmpty() ? Set.of() : ads.stream()
            .map(Ad::getClient)
            .collect(Collectors.toSet());
  }


}
