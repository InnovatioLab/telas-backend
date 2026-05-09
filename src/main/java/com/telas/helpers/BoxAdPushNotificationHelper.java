package com.telas.helpers;

import com.telas.dtos.request.UpdateBoxMonitorsAdRequestDto;
import com.telas.entities.Ad;
import com.telas.entities.Client;
import com.telas.entities.Monitor;
import com.telas.entities.MonitorAd;
import com.telas.entities.Subscription;
import com.telas.enums.NotificationReference;
import com.telas.enums.Permission;
import com.telas.repositories.ClientRepository;
import com.telas.repositories.SubscriptionRepository;
import com.telas.services.AdminEmailAlertPreferenceService;
import com.telas.services.NotificationService;
import com.telas.services.PermissionService;
import com.telas.shared.constants.SharedConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final ClientRepository clientRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PermissionService permissionService;
    private final AdminEmailAlertPreferenceService adminEmailAlertPreferenceService;

    @Value("${front.base.url}")
    private String frontBaseUrl;

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
            String clientLink = frontBaseUrl + "/client/my-telas?tab=ads";
            String adminLink = frontBaseUrl + "/admin/clients/" + client.getId() + "/messages";

            Map<String, String> clientParams = new HashMap<>();
            clientParams.put("name", client.getBusinessName());
            clientParams.put("adName", ad.getName());
            clientParams.put("link", clientLink);
            clientParams.put("monitorsSummary", monitorsSummary);
            clientParams.put("subscriptionEndsAt", subscriptionEndsAt);
            notificationService.save(NotificationReference.CLIENT_AD_DEPLOYED_TO_BOX, client, clientParams, true);

            Map<String, String> adminBase = new HashMap<>();
            adminBase.put("clientName", client.getBusinessName());
            adminBase.put("adName", ad.getName());
            adminBase.put("monitorsSummary", monitorsSummary);
            adminBase.put("subscriptionEndsAt", subscriptionEndsAt);
            adminBase.put("link", adminLink);

            for (Client recipient : clientRepository.findAllAdminsAndDevelopers()) {
                boolean canManageAds = recipient.isDeveloper()
                        || permissionService.hasPermission(recipient, Permission.ADMIN_ADS_MANAGE);
                if (!canManageAds) {
                    continue;
                }
                boolean sendEmail = !recipient.isDeveloper()
                        && adminEmailAlertPreferenceService.wantsEmail(
                                recipient.getId(), com.telas.enums.AdminEmailAlertCategory.ADS_MANAGEMENT);
                notificationService.save(
                        NotificationReference.ADMIN_CLIENT_AD_DEPLOYED_TO_BOX,
                        recipient,
                        new HashMap<>(adminBase),
                        sendEmail);
            }
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
            String addressPart = "";
            if (monitor.getAddress() != null && monitor.getAddress().resolveMapLocationDescription() != null) {
                addressPart = monitor.getAddress().resolveMapLocationDescription();
            }
            String ip = "";
            if (monitor.getBox() != null && monitor.getBox().getBoxAddress() != null
                    && monitor.getBox().getBoxAddress().getIp() != null) {
                ip = monitor.getBox().getBoxAddress().getIp();
            }
            String line = addressPart;
            if (!ip.isBlank()) {
                line = line.isBlank() ? "Box " + ip : line + " — Box " + ip;
            }
            if (line.isBlank()) {
                line = monitor.getId().toString();
            }
            lines.add(line);
        }
        return String.join("; ", lines);
    }
}
