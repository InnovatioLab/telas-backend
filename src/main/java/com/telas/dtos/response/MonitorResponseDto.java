package com.telas.dtos.response;

import com.telas.entities.Monitor;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
public final class MonitorResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -2929124221854520175L;

    private final UUID id;
    private final boolean active;
    private final AddressFromZipCodeResponseDto address;
    private final String fullAddress;
    private final boolean canBeDeleted;
    private final List<MonitorAdResponseDto> adLinks;

    private final int maxAds;
    private final int activeAdsCount;
    private final int partnerAdsCount;
    private final int clientAdsCount;
    private final int remainingTotalSlots;
    private final int remainingPartnerSlots;
    private final int remainingClientSlots;
    private final long availableAdsCount;

    public MonitorResponseDto(Monitor entity, List<MonitorAdResponseDto> adLinks, long availableAdsCount) {
        id = entity.getId();
        active = entity.isActive();
        address = new AddressFromZipCodeResponseDto(entity.getAddress());
        fullAddress = entity.getAddress().getCoordinatesParams();
        canBeDeleted = entity.getActiveSubscriptions().isEmpty();
        this.adLinks = adLinks;

        int cap = entity.getMaxBlocks() != null ? entity.getMaxBlocks() : com.telas.shared.constants.SharedConstants.MAX_MONITOR_ADS;
        maxAds = Math.min(cap, com.telas.shared.constants.SharedConstants.MAX_MONITOR_ADS);
        activeAdsCount = entity.getMonitorAds() != null ? entity.getMonitorAds().size() : 0;

        int partner = (int) entity.getMonitorAds().stream().filter(ma -> {
            var c = ma.getAd() != null ? ma.getAd().getClient() : null;
            if (c == null) return false;
            return c.isPartner() || c.isAdmin() || c.isDeveloper();
        }).count();
        partnerAdsCount = partner;
        clientAdsCount = Math.max(0, activeAdsCount - partnerAdsCount);

        int partnerCap = Math.min(10, maxAds);
        int clientCap = Math.max(0, maxAds - partnerCap);
        remainingTotalSlots = Math.max(0, maxAds - activeAdsCount);
        remainingPartnerSlots = Math.max(0, partnerCap - partnerAdsCount);
        remainingClientSlots = Math.max(0, clientCap - clientAdsCount);
        this.availableAdsCount = availableAdsCount;
    }
}