package com.telas.helpers;

import com.telas.dtos.request.MonitorAdRequestDto;
import com.telas.dtos.request.MonitorRequestDto;
import com.telas.dtos.request.RemoveBoxMonitorsAdRequestDto;
import com.telas.dtos.request.UpdateBoxMonitorsAdRequestDto;
import com.telas.dtos.response.MonitorAdResponseDto;
import com.telas.dtos.response.MonitorValidAdResponseDto;
import com.telas.entities.*;
import com.telas.enums.AdValidationType;
import com.telas.enums.Role;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.AdRepository;
import com.telas.services.AddressService;
import com.telas.services.BucketService;
import com.telas.services.GeolocationService;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.AddressValidationMessages;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.shared.utils.AttachmentUtils;
import com.telas.shared.utils.HttpClientUtil;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MonitorRequestHelper {
  private final Logger log = LoggerFactory.getLogger(MonitorRequestHelper.class);
  private final GeolocationService geolocationService;
  private final AdRepository adRepository;
  private final AddressService addressService;
  private final BucketService bucketService;
  private final HttpClientUtil httpClient;

  @Transactional
  public List<Ad> getAds(MonitorRequestDto request, UUID monitorId) {
    Set<UUID> adsIds = request.getAds().stream()
            .map(MonitorAdRequestDto::getId)
            .collect(Collectors.toSet());

    if (adsIds.size() > SharedConstants.MAX_MONITOR_ADS) {
      throw new BusinessRuleException(MonitorValidationMessages.MAX_MONITOR_ADS);
    }

    List<Ad> ads = adRepository.findAllValidAdsForMonitor(AdValidationType.APPROVED, monitorId);

    if (ads.isEmpty()) {
      return List.of();
    }

    Set<UUID> monitorAdsIds = ads.stream()
            .map(Ad::getId)
            .collect(Collectors.toSet());

    boolean hasInvalidAds = !monitorAdsIds.containsAll(adsIds);

    if (hasInvalidAds) {
      throw new BusinessRuleException(MonitorValidationMessages.AD_NOT_ABLE_TO_ASSIGN_TO_MONITOR);
    }

    return ads.stream()
            .filter(ad -> adsIds.contains(ad.getId()))
            .toList();
  }

  @Transactional
  public void setAddressCoordinates(Address address) {
    geolocationService.getAddressCoordinates(address);
    addressService.save(address);
  }

  @Transactional
  public Map<String, Double> getCoordinatesFromZipCode(String zipCode, String countryCode) {
    return geolocationService.getCoordinatesFromZipCode(zipCode, countryCode);
  }

  @Transactional
  public Address getOrCreateAddress(MonitorRequestDto request) {
    Address address = (request.getAddressId() != null)
            ? addressService.findById(request.getAddressId())
            : addressService.getOrCreateAddress(request.getAddress());

    if (address.getClient() != null && !Role.PARTNER.equals(address.getClient().getRole())) {
      throw new ResourceNotFoundException(MonitorValidationMessages.CLIENT_NOT_PARTNER);
    }

    return address;
  }

  @Transactional
  public List<String> validateZipCodeList(String zipCodes) {
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

  @Transactional
  public Set<Client> getClients(List<Ad> ads) {
    return ads.isEmpty() ? Set.of() : ads.stream()
            .map(Ad::getClient)
            .collect(Collectors.toSet());
  }

  @Transactional(readOnly = true)
  public List<MonitorValidAdResponseDto> getValidAdsForMonitor(Monitor monitor) {
    Set<MonitorAd> monitorAds = monitor.getMonitorAds();

    if (monitorAds.isEmpty()) {
      return List.of();
    }

    List<Ad> validAds = adRepository.findAllValidAdsForMonitor(AdValidationType.APPROVED, monitor.getId());

    if (validAds.isEmpty()) {
      return List.of();
    }

    return validAds.stream()
            .map(ad -> new MonitorValidAdResponseDto(
                    ad,
                    bucketService.getLink(AttachmentUtils.format(ad)),
                    monitorAds.stream().anyMatch(ma -> ma.getAd().equals(ad))
            ))
            .toList();
  }

  @Transactional(readOnly = true)
  public List<MonitorAdResponseDto> getMonitorAdsResponse(Monitor entity) {
    return entity.getMonitorAds().stream()
            .map(monitorAd -> new MonitorAdResponseDto(monitorAd, bucketService.getLink(AttachmentUtils.format(monitorAd.getAd()))))
            .toList();
  }

  @Transactional(readOnly = true)
  public Predicate createAddressPredicate(CriteriaBuilder criteriaBuilder, Root<Monitor> root, String filter) {
    return criteriaBuilder.like(
            criteriaBuilder.lower(
                    criteriaBuilder.concat(
                            criteriaBuilder.concat(
                                    criteriaBuilder.concat(
                                            criteriaBuilder.concat(
                                                    root.get("address").get("street"), " "
                                            ),
                                            root.get("address").get("city")
                                    ),
                                    " "
                            ),
                            criteriaBuilder.concat(
                                    criteriaBuilder.concat(
                                            root.get("address").get("state"), " "
                                    ),
                                    root.get("address").get("zipCode")
                            )
                    )
            ),
            filter
    );
  }

  @Transactional
  public void sendBoxesMonitorsUpdateAds(Monitor monitor, List<Ad> ads) {
    if (monitor.getBox() == null || !monitor.getBox().isActive()) {
      return;
    }

    String url = "http://" + monitor.getBox().getIp().getIpAddress() + ":5050/update-ads";

    List<UpdateBoxMonitorsAdRequestDto> dtos = ads.stream()
            .map(ad -> new UpdateBoxMonitorsAdRequestDto(
                    monitor.getId(),
                    ad.getName(),
                    bucketService.getLink(AttachmentUtils.format(ad))
            ))
            .toList();

    executePostRequest(url, dtos, monitor.getId());
  }

  @Transactional
  public void sendBoxesMonitorsRemoveAds(Monitor monitor, List<String> adNamesToRemove) {
    if (monitor.getBox() == null || !monitor.getBox().isActive()) {
      return;
    }

    String url = "http://" + monitor.getBox().getIp().getIpAddress() + ":5050/remove-ads";
    RemoveBoxMonitorsAdRequestDto dto = new RemoveBoxMonitorsAdRequestDto(monitor.getId(), adNamesToRemove);

    executePostRequest(url, dto, monitor.getId());
  }

  private <T> void executePostRequest(String url, T body, UUID monitorId) {
    try {
      httpClient.makePostRequest(url, body, Void.class, null);
    } catch (Exception e) {
      log.error("Error while sending request with monitorID: {}, URL: {}, message: {}", monitorId, url, e.getMessage());
    }
  }

  @Transactional
  public void sendBoxRemoveMonitor(Monitor monitor) {
    if (monitor.getBox() == null || !monitor.getBox().isActive()) {
      return;
    }

    String url = "http://" + monitor.getBox().getIp().getIpAddress() + ":5050/remove-monitor/" + monitor.getId().toString();
    try {
      httpClient.makeDeleteRequest(url, Void.class, null);
    } catch (Exception e) {
      log.error("Error while sending request to remove monitor with ID: {}, URL: {}, message: {}", monitor.getId(), url, e.getMessage());
      throw new BusinessRuleException("Failed to remove monitor from box: " + e.getMessage());
    }
  }
}
