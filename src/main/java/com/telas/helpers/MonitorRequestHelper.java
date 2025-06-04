package com.telas.helpers;

import com.telas.dtos.request.MonitorAdRequestDto;
import com.telas.dtos.request.MonitorRequestDto;
import com.telas.entities.Ad;
import com.telas.entities.Address;
import com.telas.enums.AdValidationType;
import com.telas.enums.Role;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.AdRepository;
import com.telas.services.AddressService;
import com.telas.services.GeolocationService;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MonitorRequestHelper {
  private final GeolocationService geolocationService;
  private final AdRepository adRepository;
  private final AddressService addressService;

  @Transactional
  public List<Ad> getAds(MonitorRequestDto request, UUID monitorId) {
    List<UUID> adsIds = request.getAds().stream()
            .map(MonitorAdRequestDto::getId)
            .toList();

    if (adsIds.size() > SharedConstants.MAX_MONITOR_ADS) {
      throw new BusinessRuleException(MonitorValidationMessages.MAX_MONITOR_ADS);
    }

    List<Ad> ads = adRepository.findAllValidAdsForMonitor(adsIds, AdValidationType.APPROVED, monitorId);

    if (ads.isEmpty()) {
      return List.of();
    }

    boolean hasInvalidAds = ads.stream().anyMatch(ad -> !adsIds.contains(ad.getId()));

    if (hasInvalidAds) {
      throw new ResourceNotFoundException(MonitorValidationMessages.AD_NOT_ABLE_TO_ASSIGN_TO_MONITOR);
    }

    return ads;
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
            : new Address(request.getAddress());

    if (address.getClient() != null && !Role.PARTNER.equals(address.getClient().getRole())) {
      throw new ResourceNotFoundException(MonitorValidationMessages.CLIENT_NOT_PARTNER);
    }

    return address;
  }
}
