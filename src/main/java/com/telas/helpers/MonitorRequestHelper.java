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
import com.telas.shared.constants.valitation.AdValidationMessages;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.shared.utils.ValidateDataUtils;
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
  public List<Ad> getAds(MonitorRequestDto request) {
    List<UUID> adsIds = request.getAds().stream()
            .map(MonitorAdRequestDto::getId)
            .toList();

    List<Ad> ads = adRepository.findAllById(
            ValidateDataUtils.isNullOrEmpty(adsIds)
                    ? List.of()
                    : adsIds
    ).orElseThrow(() -> new ResourceNotFoundException(AdValidationMessages.AD_NOT_FOUND));

    ads.removeIf(attachment -> !AdValidationType.APPROVED.equals(attachment.getValidation()));

    if (ads.size() > SharedConstants.MAX_MONITOR_ADS) {
      throw new BusinessRuleException(MonitorValidationMessages.MAX_MONITOR_ADS);
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
