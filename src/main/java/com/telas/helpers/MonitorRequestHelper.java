package com.telas.helpers;

import com.telas.dtos.request.MonitorAdRequestDto;
import com.telas.dtos.request.MonitorRequestDto;
import com.telas.dtos.response.LinkResponseDto;
import com.telas.dtos.response.MonitorAdResponseDto;
import com.telas.entities.Ad;
import com.telas.entities.Address;
import com.telas.entities.Client;
import com.telas.entities.Monitor;
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
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MonitorRequestHelper {
  private final GeolocationService geolocationService;
  private final AdRepository adRepository;
  private final AddressService addressService;
  private final BucketService bucketService;

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

//    return ads;
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
  public List<LinkResponseDto> getValidAdsForMonitor(UUID monitorId) {
    return adRepository.findAllValidAdsForMonitor(AdValidationType.APPROVED, monitorId).stream()
            .map(ad -> new LinkResponseDto(ad.getId(), bucketService.getLink(AttachmentUtils.format(ad))))
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
}
