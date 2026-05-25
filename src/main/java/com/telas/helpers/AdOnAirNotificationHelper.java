package com.telas.helpers;

import com.telas.entities.Ad;
import com.telas.entities.Client;
import com.telas.entities.Monitor;
import com.telas.entities.MonitorAd;
import com.telas.enums.AdValidationType;
import com.telas.enums.NotificationReference;
import com.telas.repositories.AdRepository;
import com.telas.services.NotificationService;
import com.telas.services.notification.AdminAdsNotificationService;
import com.telas.shared.utils.ClientPortalLinkResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AdOnAirNotificationHelper {

    private final NotificationService notificationService;
    private final AdminAdsNotificationService adminAdsNotificationService;
    private final AdRepository adRepository;
    private final ClientPortalLinkResolver clientPortalLinkResolver;

    public void notifyOnAirForNewMonitorAds(List<MonitorAd> newMonitorAds, Monitor monitor) {
        notifyOnAirForNewMonitorAds(newMonitorAds, monitor, true, true);
    }

    public void notifyOnAirForNewMonitorAds(List<MonitorAd> newMonitorAds, Monitor monitor, boolean sendEmailNotifications) {
        notifyOnAirForNewMonitorAds(newMonitorAds, monitor, sendEmailNotifications, true);
    }

    public void notifyOnAirForNewMonitorAds(
            List<MonitorAd> newMonitorAds,
            Monitor monitor,
            boolean sendEmailNotifications,
            boolean notifyClient
    ) {
        if (newMonitorAds == null || newMonitorAds.isEmpty()) {
            return;
        }
        if (monitor == null || !monitor.isAbleToSendBoxRequest()) {
            return;
        }
        for (MonitorAd ma : newMonitorAds) {
            Ad ad = ma != null ? ma.getAd() : null;
            Client client = ad != null ? ad.getClient() : null;
            if (ad == null || client == null) {
                continue;
            }
            if (!AdValidationType.APPROVED.equals(ad.getValidation())) {
                continue;
            }
            if (ad.getOnAirNotifiedAt() != null) {
                continue;
            }

            ad.setOnAirNotifiedAt(Instant.now());
            adRepository.save(ad);

            if (notifyClient) {
                Map<String, String> clientParams = new HashMap<>();
                clientParams.put("name", client.getBusinessName());
                clientParams.put("adName", ad.getName());
                clientParams.put("link", clientPortalLinkResolver.clientAdsTabLink(client));
                clientParams.put("partner", clientPortalLinkResolver.partnerFlag(client));
                notificationService.save(NotificationReference.CLIENT_AD_ON_AIR, client, clientParams, sendEmailNotifications);
            }

            Map<String, String> adminParams = new HashMap<>();
            adminParams.put("clientName", client.getBusinessName());
            adminParams.put("adName", ad.getName());
            adminParams.put("link", clientPortalLinkResolver.adminClientMessagesLink(client.getId()));
            adminAdsNotificationService.notifyAdmins(
                    NotificationReference.ADMIN_AD_ON_AIR,
                    adminParams,
                    sendEmailNotifications
            );
        }
    }
}
