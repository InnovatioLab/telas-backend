package com.telas.helpers;

import com.telas.dtos.request.UpdateBoxMonitorsAdRequestDto;
import com.telas.entities.Ad;
import com.telas.entities.Client;
import com.telas.entities.Monitor;
import com.telas.entities.MonitorAd;
import com.telas.entities.Subscription;
import com.telas.enums.NotificationReference;
import com.telas.repositories.SubscriptionRepository;
import com.telas.services.NotificationService;
import com.telas.services.notification.AdminAdsNotificationService;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.utils.ClientPortalLinkResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BoxAdPushNotificationHelper {

    private static final DateTimeFormatter ENDS_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm z")
            .withZone(ZoneId.of(SharedConstants.ZONE_ID));

    private final NotificationService notificationService;
    private final AdminAdsNotificationService adminAdsNotificationService;
    private final SubscriptionRepository subscriptionRepository;
    private final ClientPortalLinkResolver clientPortalLinkResolver;
    private final MonitorSummaryFormatter monitorSummaryFormatter;

    public void notifyAfterSuccessfulPush(
            Map<Monitor, List<AbstractMap.SimpleEntry<MonitorAd, UpdateBoxMonitorsAdRequestDto>>> grouped,
            Set<String> successfulBaseUrls) {
        if (grouped == null || grouped.isEmpty() || successfulBaseUrls == null || successfulBaseUrls.isEmpty()) {
            return;
        }

        Set<Ad> ads = new HashSet<>();
        for (Map.Entry<Monitor, List<AbstractMap.SimpleEntry<MonitorAd, UpdateBoxMonitorsAdRequestDto>>> e : grouped.entrySet()) {
            for (AbstractMap.SimpleEntry<MonitorAd, UpdateBoxMonitorsAdRequestDto> pair : e.getValue()) {
                UpdateBoxMonitorsAdRequestDto dto = pair.getValue();
                MonitorAd ma = pair.getKey();
                if (dto == null || ma == null || ma.getAd() == null) {
                    continue;
                }
                if (successfulBaseUrls.contains(dto.getBaseUrl())) {
                    ads.add(ma.getAd());
                }
            }
        }

        for (Ad ad : ads) {
            Client client = ad.getClient();
            if (client == null) {
                continue;
            }
            String monitorsSummary = buildMonitorsSummaryForAd(grouped, successfulBaseUrls, ad.getId());
            String subscriptionEndsAt = formatSubscriptionEnds(client.getId());
            notifyDeployed(ad, client, monitorsSummary);
        }
    }

    private String formatSubscriptionEnds(UUID clientId) {
        List<Subscription> subs = subscriptionRepository.findActiveSubscriptionsByClientId(clientId);
        if (subs.isEmpty()) {
            return "";
        }
        Instant latest = subs.stream()
                .map(Subscription::getEndsAt)
                .filter(Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);
        if (latest == null) {
            return "";
        }
        return ENDS_FORMAT.format(latest);
    }

    private String buildMonitorsSummaryForAd(
            Map<Monitor, List<AbstractMap.SimpleEntry<MonitorAd, UpdateBoxMonitorsAdRequestDto>>> grouped,
            Set<String> successfulBaseUrls,
            UUID adId) {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<Monitor, List<AbstractMap.SimpleEntry<MonitorAd, UpdateBoxMonitorsAdRequestDto>>> e : grouped.entrySet()) {
            Monitor monitor = e.getKey();
            boolean relevant = e.getValue().stream().anyMatch(pair -> {
                UpdateBoxMonitorsAdRequestDto dto = pair.getValue();
                MonitorAd ma = pair.getKey();
                return dto != null
                        && successfulBaseUrls.contains(dto.getBaseUrl())
                        && ma.getAd() != null
                        && adId.equals(ma.getAd().getId());
            });
            if (!relevant) {
                continue;
            }
            lines.add(monitorSummaryFormatter.formatMonitorLine(monitor));
        }
        return String.join("; ", lines);
    }

    public void notifyAfterAdStagedToBox(Ad ad, List<Monitor> monitors) {
        if (ad == null || ad.getClient() == null) {
            return;
        }
        String monitorsSummary = monitorSummaryFormatter.formatMonitorsSummary(monitors);
        notifyDeployed(ad, ad.getClient(), monitorsSummary);
    }

    private void notifyDeployed(Ad ad, Client client, String monitorsSummary) {
        String subscriptionEndsAt = formatSubscriptionEnds(client.getId());

        Map<String, String> clientParams = new HashMap<>();
        clientParams.put("name", client.getBusinessName());
        clientParams.put("adName", ad.getName());
        clientParams.put("link", clientPortalLinkResolver.clientAdsTabLink(client));
        clientParams.put("partner", clientPortalLinkResolver.partnerFlag(client));
        clientParams.put("monitorsSummary", monitorsSummary);
        clientParams.put("subscriptionEndsAt", subscriptionEndsAt);
        notificationService.save(NotificationReference.CLIENT_AD_DEPLOYED_TO_BOX, client, clientParams, true);

        Map<String, String> adminBase = new HashMap<>();
        adminBase.put("clientName", client.getBusinessName());
        adminBase.put("adName", ad.getName());
        adminBase.put("monitorsSummary", monitorsSummary);
        adminBase.put("subscriptionEndsAt", subscriptionEndsAt);
        adminBase.put("link", clientPortalLinkResolver.adminClientMessagesLink(client.getId()));
        adminBase.put("actorType", client.isPartner() ? "partner" : "customer");
        adminAdsNotificationService.notifyAdmins(NotificationReference.ADMIN_CLIENT_AD_DEPLOYED_TO_BOX, adminBase);
    }
}
