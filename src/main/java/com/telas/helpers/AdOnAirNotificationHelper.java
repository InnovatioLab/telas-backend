package com.telas.helpers;

import com.telas.entities.Ad;
import com.telas.entities.Client;
import com.telas.entities.Monitor;
import com.telas.entities.MonitorAd;
import com.telas.enums.AdValidationType;
import com.telas.enums.NotificationReference;
import com.telas.enums.Permission;
import com.telas.repositories.AdRepository;
import com.telas.repositories.ClientRepository;
import com.telas.services.AdminEmailAlertPreferenceService;
import com.telas.services.NotificationService;
import com.telas.services.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AdOnAirNotificationHelper {

    private final NotificationService notificationService;
    private final ClientRepository clientRepository;
    private final PermissionService permissionService;
    private final AdminEmailAlertPreferenceService adminEmailAlertPreferenceService;
    private final AdRepository adRepository;

    @Value("${front.base.url}")
    private String frontBaseUrl;

    public void notifyOnAirForNewMonitorAds(List<MonitorAd> newMonitorAds, Monitor monitor) {
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

            Map<String, String> clientParams = new HashMap<>();
            clientParams.put("name", client.getBusinessName());
            clientParams.put("adName", ad.getName());
            clientParams.put("link", frontBaseUrl + "/client/my-telas?tab=ads");
            notificationService.save(NotificationReference.CLIENT_AD_ON_AIR, client, clientParams, true);

            String adminLink = frontBaseUrl + "/admin/clients/" + client.getId() + "/messages";
            Map<String, String> adminParams = new HashMap<>();
            adminParams.put("clientName", client.getBusinessName());
            adminParams.put("adName", ad.getName());
            adminParams.put("link", adminLink);
            for (Client recipient : clientRepository.findAllAdminsAndDevelopers()) {
                boolean canManageAds = recipient.isDeveloper()
                        || permissionService.hasPermission(recipient, Permission.ADMIN_ADS_MANAGE);
                if (!canManageAds) {
                    continue;
                }
                boolean sendEmail = adminEmailAlertPreferenceService.wantsEmail(
                        recipient.getId(), com.telas.enums.AdminEmailAlertCategory.ADS_MANAGEMENT);
                notificationService.save(
                        NotificationReference.ADMIN_AD_ON_AIR,
                        recipient,
                        new HashMap<>(adminParams),
                        sendEmail
                );
            }
        }
    }
}

