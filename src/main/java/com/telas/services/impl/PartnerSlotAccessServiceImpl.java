package com.telas.services.impl;

import com.telas.entities.Ad;
import com.telas.entities.CartItem;
import com.telas.entities.Client;
import com.telas.entities.Monitor;
import com.telas.entities.MonitorAd;
import com.telas.entities.SubscriptionMonitor;
import com.telas.enums.AdValidationType;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.services.PartnerPlatformSettingsService;
import com.telas.services.PartnerSlotAccessService;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.ClientValidationMessages;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PartnerSlotAccessServiceImpl implements PartnerSlotAccessService {

    private final PartnerPlatformSettingsService partnerPlatformSettingsService;

    @Override
    public boolean hasGlobalSlotsPermission(Client client) {
        return client != null
                && client.isPartner()
                && partnerPlatformSettingsService.isSlotsAnyLocationEnabled();
    }

    @Override
    public boolean usesPartnerQuotaOnMonitor(Client client, Monitor monitor) {
        if (client == null || monitor == null) {
            return false;
        }
        return monitor.isPartner(client) || hasGlobalSlotsPermission(client);
    }

    @Override
    public int maxBlocksForClientOnMonitor(Client client, Monitor monitor) {
        return usesPartnerQuotaOnMonitor(client, monitor)
                ? SharedConstants.PARTNER_RESERVED_SLOTS
                : SharedConstants.MIN_QUANTITY_MONITOR_BLOCK;
    }

    @Override
    public int usedBlocksByClientOnMonitor(Client client, Monitor monitor) {
        if (client == null || monitor == null) {
            return 0;
        }

        int fromAds = monitor.getMonitorAds().stream()
                .filter(ma -> isAdOwnedByClient(ma, client))
                .mapToInt(ma -> normalizeBlockQuantity(ma.getBlockQuantity()))
                .sum();

        int fromSubscriptions = monitor.getActiveSubscriptionMonitors().stream()
                .filter(sm -> sm.getSubscription() != null
                        && sm.getSubscription().getClient() != null
                        && client.getId().equals(sm.getSubscription().getClient().getId()))
                .mapToInt(sm -> normalizeBlockQuantity(sm.getSlotsQuantity()))
                .sum();

        return fromAds + fromSubscriptions;
    }

    @Override
    public boolean canAddBlocks(Client client, Monitor monitor, int additionalBlocks) {
        if (client == null || monitor == null || additionalBlocks <= 0) {
            return false;
        }

        int monitorCap = resolveMonitorCap(monitor);
        int totalUsed = totalBlocksUsedOnMonitor(monitor);

        if (totalUsed + additionalBlocks > monitorCap) {
            return false;
        }

        if (usesPartnerQuotaOnMonitor(client, monitor)) {
            return usedBlocksByClientOnMonitor(client, monitor) + additionalBlocks
                    <= SharedConstants.PARTNER_RESERVED_SLOTS;
        }

        CartItem probe = new CartItem();
        probe.setBlockQuantity(additionalBlocks);
        return monitor.hasAvailableBlocks(probe);
    }

    @Override
    public void assertCanAddBlocks(Client client, Monitor monitor, int additionalBlocks) {
        if (!canAddBlocks(client, monitor, additionalBlocks)) {
            throw new BusinessRuleException(MonitorValidationMessages.MONITOR_BLOCKS_UNAVAILABLE);
        }
    }

    @Override
    public void validatePartnerAdCreationAllowed(Client partner, Monitor monitor, boolean createsAdEntity) {
        if (partner == null || !partner.isPartner() || !createsAdEntity) {
            return;
        }
        if (usesPartnerQuotaOnMonitor(partner, monitor)) {
            return;
        }
        long activeAds = partner.getAds().stream()
                .filter(this::countsTowardPartnerActiveAdLimit)
                .count();
        if (activeAds >= SharedConstants.MAX_ADS_PER_CLIENT) {
            throw new BusinessRuleException(ClientValidationMessages.MAX_ADS_REACHED);
        }
    }

    private boolean countsTowardPartnerActiveAdLimit(Ad ad) {
        AdValidationType validation = ad.getValidation();
        return AdValidationType.APPROVED.equals(validation) || AdValidationType.PENDING.equals(validation);
    }

    @Override
    public int resolveCartBlockQuantity(Client client, Monitor monitor, Integer requestedQuantity) {
        int max = maxBlocksForClientOnMonitor(client, monitor);
        if (!usesPartnerQuotaOnMonitor(client, monitor)) {
            return SharedConstants.MIN_QUANTITY_MONITOR_BLOCK;
        }
        if (monitor.isPartner(client)) {
            return SharedConstants.PARTNER_RESERVED_SLOTS;
        }
        if (hasGlobalSlotsPermission(client)) {
            int requested = requestedQuantity != null ? requestedQuantity : max;
            if (requested <= SharedConstants.MIN_QUANTITY_MONITOR_BLOCK) {
                return max;
            }
            return Math.min(requested, max);
        }
        return SharedConstants.MIN_QUANTITY_MONITOR_BLOCK;
    }

    private static int resolveMonitorCap(Monitor monitor) {
        int cap = monitor.getMaxBlocks() != null ? monitor.getMaxBlocks() : SharedConstants.MAX_MONITOR_ADS;
        return Math.min(cap, SharedConstants.MAX_MONITOR_ADS);
    }

    private static int totalBlocksUsedOnMonitor(Monitor monitor) {
        int fromAds = monitor.getMonitorAds().stream()
                .mapToInt(ma -> normalizeBlockQuantity(ma.getBlockQuantity()))
                .sum();

        int fromSubscriptions = monitor.getActiveSubscriptionMonitors().stream()
                .mapToInt(sm -> normalizeBlockQuantity(sm.getSlotsQuantity()))
                .sum();

        return Math.max(fromAds, fromSubscriptions);
    }

    private static boolean isAdOwnedByClient(MonitorAd monitorAd, Client client) {
        return monitorAd.getAd() != null
                && monitorAd.getAd().getClient() != null
                && client.getId().equals(monitorAd.getAd().getClient().getId());
    }

    private static int normalizeBlockQuantity(Integer quantity) {
        return quantity != null && quantity > 0 ? quantity : SharedConstants.MIN_QUANTITY_MONITOR_BLOCK;
    }
}
