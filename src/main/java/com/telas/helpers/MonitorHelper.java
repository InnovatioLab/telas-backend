package com.telas.helpers;

import com.telas.dtos.request.MonitorAdRequestDto;
import com.telas.dtos.request.MonitorRequestDto;
import com.telas.dtos.request.RemoveBoxMonitorsAdRequestDto;
import com.telas.dtos.request.UpdateBoxMonitorsAdRequestDto;
import com.telas.dtos.response.MonitorAdResponseDto;
import com.telas.dtos.response.MonitorValidAdResponseDto;
import com.telas.entities.Ad;
import com.telas.entities.Address;
import com.telas.entities.Monitor;
import com.telas.entities.MonitorAd;
import com.telas.enums.AdValidationType;
import com.telas.enums.Role;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.repositories.AdRepository;
import com.telas.repositories.MonitorRepository;
import com.telas.services.AddressService;
import com.telas.services.BucketService;
import com.telas.services.MapsService;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MonitorHelper {
    private final Logger log = LoggerFactory.getLogger(MonitorHelper.class);
    private final MapsService mapsService;
    private final AdRepository adRepository;
    private final MonitorRepository repository;
    private final AddressService addressService;
    private final BucketService bucketService;
    private final HttpClientUtil httpClient;

    @Value("${TOKEN_SECRET}")
    private String API_KEY;

    @Transactional
    public List<Ad> getAds(MonitorRequestDto request, UUID monitorId) {
        Set<UUID> adsIds = request.getAds().stream()
                .map(MonitorAdRequestDto::getId)
                .collect(Collectors.toSet());

        List<Ad> ads = adRepository.findAllValidAdsForMonitor(AdValidationType.APPROVED, monitorId);

        if (ads.isEmpty()) {
            return Collections.emptyList();
        }

        Set<UUID> monitorAdsIds = ads.stream()
                .map(Ad::getId)
                .collect(Collectors.toSet());

        if (!monitorAdsIds.containsAll(adsIds)) {
            throw new BusinessRuleException(MonitorValidationMessages.AD_NOT_ABLE_TO_ASSIGN_TO_MONITOR);
        }

        return ads.stream()
                .filter(ad -> adsIds.contains(ad.getId()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void setAddressCoordinates(Address address) {
        mapsService.getAddressCoordinates(address);
        addressService.save(address);
    }

    @Transactional
    public Address getOrCreateAddress(MonitorRequestDto request) {
        Address address = (request.getAddressId() != null)
                ? addressService.findById(request.getAddressId())
                : addressService.getOrCreateAddress(request.getAddress());

        if (address.getClient() != null && !Role.PARTNER.equals(address.getClient().getRole())) {
            throw new BusinessRuleException(MonitorValidationMessages.CLIENT_NOT_PARTNER);
        }

        return address;
    }

    @Transactional
    public void validateZipCodeList(String zipCode) {
        if (!zipCode.matches(SharedConstants.REGEX_ZIP_CODE)) {
            throw new BusinessRuleException(AddressValidationMessages.ZIP_CODE_INVALID);
        }
    }

    @Transactional(readOnly = true)
    public List<MonitorValidAdResponseDto> getValidAdsForMonitor(Monitor monitor) {
        List<Ad> validAds = adRepository.findAllValidAdsForMonitor(AdValidationType.APPROVED, monitor.getId());

        if (validAds.isEmpty()) {
            return List.of();
        }

        Set<MonitorAd> monitorAds = monitor.getMonitorAds();

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


    public void sendBoxesMonitorsUpdateAds(Monitor monitor, List<Ad> ads) {
        String url = "http://" + monitor.getBox().getBoxAddress().getIp() + ":8081/update-ads";

        List<UpdateBoxMonitorsAdRequestDto> dtos = ads.stream()
                .map(ad -> new UpdateBoxMonitorsAdRequestDto(
                        monitor.getId(),
                        ad.getName(),
                        bucketService.getLink(AttachmentUtils.format(ad))
                ))
                .toList();

        log.info("Sending request to update boxMonitorsAds for monitor with ID: {}, URL: {}", monitor.getId(), url);
        executePostRequest(url, dtos, monitor.getId());
    }

    public void sendBoxesMonitorsRemoveAds(Monitor monitor, List<String> adNamesToRemove) {
        String url = String.format("http://%s:8081/remove-ads", monitor.getBox().getBoxAddress().getIp());
        RemoveBoxMonitorsAdRequestDto dto = new RemoveBoxMonitorsAdRequestDto(monitor.getId(), adNamesToRemove);

        log.info("Sending request to remove ads from boxMonitorsAds for monitor with ID: {}, URL: {}", monitor.getId(), url);
        executePostRequest(url, dto, monitor.getId());
    }

    @Transactional
    public void sendBoxesMonitorsRemoveAd(Ad ad, List<String> adNameToRemove) {
        List<Monitor> activeMonitorsToUpdate = getClientMonitorsWithActiveSubscription(ad.getClient().getId()).stream()
                .filter(monitor -> monitor.getMonitorAds().stream()
                        .anyMatch(monitorAd -> monitorAd.getAd().getId().equals(ad.getId())))
                .toList();

        activeMonitorsToUpdate.forEach(monitor -> {
            monitor.getMonitorAds().removeIf(monitorAd -> monitorAd.getAd().getId().equals(ad.getId()));
            repository.save(monitor);
        });

        activeMonitorsToUpdate.stream()
                .filter(Monitor::isAbleToSendBoxRequest)
                .forEach(monitor -> sendBoxesMonitorsRemoveAds(monitor, adNameToRemove));
    }

    private <T> void executePostRequest(String url, T body, UUID monitorId) {
        try {
            Map<String, String> headers = Map.of("X-API-KEY", API_KEY);
            httpClient.makePostRequest(url, body, Void.class, null, headers);
        } catch (Exception e) {
            log.error("Error while sending request with monitorID: {}, URL: {}, message: {}", monitorId, url, e.getMessage());
        }
    }

    @Transactional
    public void sendBoxRemoveMonitor(Monitor monitor) {
        if (!monitor.isAbleToSendBoxRequest()) {
            return;
        }

        String url = "http://" + monitor.getBox().getBoxAddress().getIp() + ":8081/remove-monitor/" + monitor.getId().toString();
        try {
            log.info("Sending request to remove monitor with ID: {}, URL: {}", monitor.getId(), url);
            Map<String, String> headers = Map.of("X-API-KEY", API_KEY);
            httpClient.makeDeleteRequest(url, null, headers);
        } catch (Exception e) {
            log.error("Error while sending request to remove monitor with ID: {}, URL: {}, message: {}", monitor.getId(), url, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<MonitorValidAdResponseDto> getBoxMonitorAdsResponse(Monitor monitor, List<String> adNames) {
        return monitor.getAds().stream()
                .filter(ad -> adNames.contains(ad.getName()))
                .map(ad -> {
                    String adLink = bucketService.getLink(AttachmentUtils.format(ad));
                    return new MonitorValidAdResponseDto(ad, adLink, true);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getCurrentDisplayedAdsFromBox(Monitor monitor) {
        if (!monitor.isAbleToSendBoxRequest()) {
            return Collections.emptyList();
        }

        String url = "http://" + monitor.getBox().getBoxAddress().getIp() + ":8081/get-ads/" + monitor.getId().toString();
        try {
            log.info("Sending request to get current displayed ads from box for monitor with ID: {}, URL: {}", monitor.getId(), url);
            return (List<String>) httpClient.makeGetRequest(url, List.class, null);
        } catch (Exception e) {
            log.error("Error while sending request to get current displayed ads from box for monitor with ID: {}, URL: {}, message: {}", monitor.getId(), url, e.getMessage());
            throw e;
        }
    }

    private List<Monitor> getClientMonitorsWithActiveSubscription(UUID clientId) {
        return repository.findMonitorsWithActiveSubscriptionsByClientId(clientId);
    }
}
