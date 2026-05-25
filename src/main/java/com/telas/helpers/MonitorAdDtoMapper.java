package com.telas.helpers;

import com.telas.dtos.response.MonitorAdResponseDto;
import com.telas.dtos.response.MonitorValidAdResponseDto;
import com.telas.entities.Ad;
import com.telas.entities.Monitor;
import com.telas.entities.MonitorAd;
import com.telas.entities.SubscriptionMonitor;
import com.telas.enums.AdValidationType;
import com.telas.enums.PartnerAdDeploymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MonitorAdDtoMapper {

    private final AdMediaLinkFactory adMediaLinkFactory;

    public List<MonitorValidAdResponseDto> toValidAdDtos(List<Ad> validAds) {
        if (validAds.isEmpty()) {
            return List.of();
        }
        return validAds.stream()
                .map(ad -> new MonitorValidAdResponseDto(ad, adMediaLinkFactory.getLink(ad), false, null))
                .toList();
    }

    public List<MonitorAdResponseDto> toPartnerAdvertiserAdsOnMonitor(
            Monitor entity,
            UUID partnerId,
            Map<UUID, SubscriptionMonitor> activeSubscriptionByClientId) {
        if (entity.getMonitorAds() == null || partnerId == null) {
            return List.of();
        }
        return entity.getMonitorAds().stream()
                .filter(ma -> ma.getAd() != null
                        && ma.getAd().getClient() != null
                        && partnerId.equals(ma.getAd().getClient().getId())
                        && ma.getAd().getOnAirNotifiedAt() != null
                        && AdValidationType.APPROVED.equals(ma.getAd().getValidation()))
                .map(monitorAd -> enrichWithSubscription(toMonitorAdResponseDto(monitorAd), monitorAd, activeSubscriptionByClientId))
                .toList();
    }

    public MonitorAdResponseDto toPartnerPortalAdDto(MonitorAd monitorAd, Ad ad) {
        String adLink = adMediaLinkFactory.getLink(ad);
        MonitorAdResponseDto dto = monitorAd != null
                ? toMonitorAdResponseDto(monitorAd)
                : buildPortalAdDtoWithoutPlacement(ad, adLink);
        if (ad.getValidation() != null) {
            dto.setValidation(ad.getValidation().name());
        }
        if (ad.getOnAirNotifiedAt() != null) {
            dto.setOnAirSince(ad.getOnAirNotifiedAt());
        }
        PartnerAdDeploymentStatus status = resolvePartnerDeploymentStatus(ad, monitorAd);
        dto.setDeploymentStatus(status.name());
        dto.setCanRequestRemoval(monitorAd != null || ad.getOnAirNotifiedAt() != null);
        return dto;
    }

    public List<MonitorAdResponseDto> toMonitorAdsResponse(
            Monitor entity,
            Map<UUID, SubscriptionMonitor> activeSubscriptionByClientId) {
        return entity.getMonitorAds().stream()
                .map(monitorAd -> enrichWithSubscription(toMonitorAdResponseDto(monitorAd), monitorAd, activeSubscriptionByClientId))
                .toList();
    }

    public List<MonitorValidAdResponseDto> toBoxMonitorAdsResponse(Monitor monitor, List<String> adNames) {
        return monitor.getMonitorAds().stream()
                .filter(monitorAd -> adNames.contains(monitorAd.getAd().getName()))
                .map(monitorAd -> {
                    Ad ad = monitorAd.getAd();
                    String adLink = adMediaLinkFactory.getLink(ad);
                    return new MonitorValidAdResponseDto(ad, adLink, true, monitorAd.getOrderIndex());
                })
                .sorted(Comparator.comparing(MonitorValidAdResponseDto::getOrderIndex))
                .collect(Collectors.toList());
    }

    public String getAdLink(Ad ad) {
        return adMediaLinkFactory.getLink(ad);
    }

    public MonitorAdResponseDto toMonitorAdResponseDto(MonitorAd monitorAd) {
        MonitorAdResponseDto dto = new MonitorAdResponseDto(
                monitorAd,
                adMediaLinkFactory.getLink(monitorAd.getAd())
        );
        if (monitorAd.getAd().getValidation() != null) {
            dto.setValidation(monitorAd.getAd().getValidation().name());
        }
        if (monitorAd.getAd().getOnAirNotifiedAt() != null) {
            dto.setOnAirSince(monitorAd.getAd().getOnAirNotifiedAt());
        }
        return dto;
    }

    private MonitorAdResponseDto enrichWithSubscription(
            MonitorAdResponseDto dto,
            MonitorAd monitorAd,
            Map<UUID, SubscriptionMonitor> activeSubscriptionByClientId) {
        UUID clientId = monitorAd.getAd() != null && monitorAd.getAd().getClient() != null
                ? monitorAd.getAd().getClient().getId()
                : null;
        if (clientId != null) {
            SubscriptionMonitor sm = activeSubscriptionByClientId.get(clientId);
            if (sm != null) {
                dto.setSubscriptionEndsAt(sm.getId().getSubscription().getEndsAt());
            }
        }
        return dto;
    }

    private static MonitorAdResponseDto buildPortalAdDtoWithoutPlacement(Ad ad, String adLink) {
        MonitorAdResponseDto dto = new MonitorAdResponseDto();
        dto.setId(ad.getId());
        dto.setLink(adLink);
        dto.setFileName(ad.getName());
        dto.setClientName(ad.getClient() != null ? ad.getClient().getBusinessName() : null);
        return dto;
    }

    private static PartnerAdDeploymentStatus resolvePartnerDeploymentStatus(Ad ad, MonitorAd monitorAd) {
        if (ad.getOnAirNotifiedAt() != null) {
            return PartnerAdDeploymentStatus.ON_AIR;
        }
        if (ad.getPartnerBoxStagedAt() != null) {
            return PartnerAdDeploymentStatus.STAGED;
        }
        return PartnerAdDeploymentStatus.APPROVED_PENDING;
    }
}
