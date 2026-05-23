package com.telas.helpers;

import com.telas.dtos.request.UpdateBoxMonitorsAdRequestDto;
import com.telas.entities.Ad;
import com.telas.entities.Client;
import com.telas.entities.Monitor;
import com.telas.entities.MonitorAd;
import com.telas.enums.NotificationReference;
import com.telas.enums.Permission;
import com.telas.repositories.ClientRepository;
import com.telas.services.AdminEmailAlertPreferenceService;
import com.telas.services.NotificationService;
import com.telas.services.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AdPublicationNotificationHelper {

    private final BoxAdPushNotificationHelper boxAdPushNotificationHelper;
    private final AdOnAirNotificationHelper adOnAirNotificationHelper;
    private final NotificationService notificationService;
    private final ClientRepository clientRepository;
    private final PermissionService permissionService;
    private final AdminEmailAlertPreferenceService adminEmailAlertPreferenceService;

    @Value("${front.base.url}")
    private String frontBaseUrl;

    public void notifyAfterPlaylistUpdate(
            Monitor monitor,
            List<MonitorAd> newMonitorAds,
            Set<String> successfulBaseUrls,
            List<UpdateBoxMonitorsAdRequestDto> requestList) {
        if (newMonitorAds == null || newMonitorAds.isEmpty() || monitor == null) {
            return;
        }
        Set<String> synced = successfulBaseUrls != null ? successfulBaseUrls : Set.of();
        boolean boxSyncSucceeded = !synced.isEmpty();

        if (boxSyncSucceeded) {
            List<AbstractMap.SimpleEntry<MonitorAd, UpdateBoxMonitorsAdRequestDto>> pairs =
                    buildDeployNotifyPairs(monitor, newMonitorAds, requestList, synced);
            if (!pairs.isEmpty()) {
                boxAdPushNotificationHelper.notifyAfterSuccessfulPush(Map.of(monitor, pairs), synced);
            } else {
                notifyDeployedForNewAds(monitor, newMonitorAds);
            }
        } else {
            notifyPlaylistSavedPendingBoxSync(monitor, newMonitorAds);
        }

        adOnAirNotificationHelper.notifyOnAirForNewMonitorAds(
                newMonitorAds,
                monitor,
                boxSyncSucceeded);
    }

    public void notifyDeployedForNewAds(Monitor monitor, List<MonitorAd> newMonitorAds) {
        Set<UUID> notified = new HashSet<>();
        for (MonitorAd ma : newMonitorAds) {
            if (ma == null || ma.getAd() == null || !notified.add(ma.getAd().getId())) {
                continue;
            }
            boxAdPushNotificationHelper.notifyAfterAdStagedToBox(ma.getAd(), List.of(monitor));
        }
    }

    private void notifyPlaylistSavedPendingBoxSync(Monitor monitor, List<MonitorAd> newMonitorAds) {
        String monitorsSummary = formatMonitorLine(monitor);
        for (MonitorAd ma : newMonitorAds) {
            Ad ad = ma.getAd();
            Client client = ad != null ? ad.getClient() : null;
            if (ad == null || client == null) {
                continue;
            }
            String clientLink = client.isPartner()
                    ? frontBaseUrl + "/client/screens"
                    : frontBaseUrl + "/client/my-telas?tab=ads";
            Map<String, String> clientParams = new HashMap<>();
            clientParams.put("name", client.getBusinessName());
            clientParams.put("adName", ad.getName());
            clientParams.put("link", clientLink);
            clientParams.put("partner", client.isPartner() ? "true" : "false");
            clientParams.put("monitorsSummary", monitorsSummary);
            notificationService.save(NotificationReference.AD_ADDED_TO_PLAYLIST_PENDING_SYNC, client, clientParams, true);

            Map<String, String> adminBase = new HashMap<>();
            adminBase.put("clientName", client.getBusinessName());
            adminBase.put("adName", ad.getName());
            adminBase.put("monitorsSummary", monitorsSummary);
            adminBase.put("link", frontBaseUrl + "/admin/clients/" + client.getId() + "/messages");
            notifyAdmins(NotificationReference.ADMIN_AD_ADDED_TO_PLAYLIST_PENDING_SYNC, adminBase);
        }
    }

    private void notifyAdmins(NotificationReference reference, Map<String, String> adminBase) {
        for (Client recipient : clientRepository.findAllAdminsAndDevelopers()) {
            boolean canManageAds = recipient.isDeveloper()
                    || permissionService.hasPermission(recipient, Permission.ADMIN_ADS_MANAGE);
            if (!canManageAds) {
                continue;
            }
            boolean sendEmail = !recipient.isDeveloper()
                    && adminEmailAlertPreferenceService.wantsEmail(
                            recipient.getId(), com.telas.enums.AdminEmailAlertCategory.ADS_MANAGEMENT);
            notificationService.save(reference, recipient, new HashMap<>(adminBase), sendEmail);
        }
    }

    private String formatMonitorLine(Monitor monitor) {
        if (monitor == null) {
            return "";
        }
        String addressPart = monitor.getAddress() != null
                ? monitor.getAddress().resolveMapLocationDescription()
                : "";
        String ip = monitor.getBox() != null && monitor.getBox().getBoxAddress() != null
                ? monitor.getBox().getBoxAddress().getIp()
                : "";
        if (addressPart != null && !addressPart.isBlank() && ip != null && !ip.isBlank()) {
            return addressPart + " — Box " + ip;
        }
        if (addressPart != null && !addressPart.isBlank()) {
            return addressPart;
        }
        if (ip != null && !ip.isBlank()) {
            return "Box " + ip;
        }
        return monitor.getId() != null ? monitor.getId().toString() : "";
    }

    private static List<AbstractMap.SimpleEntry<MonitorAd, UpdateBoxMonitorsAdRequestDto>> buildDeployNotifyPairs(
            Monitor monitor,
            List<MonitorAd> newMonitorAds,
            List<UpdateBoxMonitorsAdRequestDto> requestList,
            Set<String> successfulBaseUrls) {
        if (requestList == null || requestList.isEmpty() || successfulBaseUrls.isEmpty()) {
            return List.of();
        }
        Set<UUID> newAdIds = newMonitorAds.stream()
                .map(ma -> ma.getAd() != null ? ma.getAd().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<AbstractMap.SimpleEntry<MonitorAd, UpdateBoxMonitorsAdRequestDto>> allPairs = new ArrayList<>();
        for (UpdateBoxMonitorsAdRequestDto dto : requestList) {
            if (dto == null || dto.getFileName() == null) {
                continue;
            }
            MonitorAd ma = monitor.getMonitorAds().stream()
                    .filter(x -> x.getAd() != null && newAdIds.contains(x.getAd().getId()))
                    .filter(x -> dto.getFileName().equals(x.getAd().getName())
                            || dto.getFileName().equalsIgnoreCase(x.getAd().getName()))
                    .findFirst()
                    .orElse(null);
            if (ma != null && dto.getBaseUrl() != null && successfulBaseUrls.contains(dto.getBaseUrl())) {
                allPairs.add(new AbstractMap.SimpleEntry<>(ma, dto));
            }
        }
        if (!allPairs.isEmpty()) {
            return allPairs;
        }

        UpdateBoxMonitorsAdRequestDto fallbackDto = requestList.stream()
                .filter(Objects::nonNull)
                .filter(d -> d.getBaseUrl() != null && successfulBaseUrls.contains(d.getBaseUrl()))
                .findFirst()
                .orElse(null);
        if (fallbackDto == null) {
            return List.of();
        }
        List<AbstractMap.SimpleEntry<MonitorAd, UpdateBoxMonitorsAdRequestDto>> fallback = new ArrayList<>();
        for (MonitorAd ma : newMonitorAds) {
            if (ma.getAd() != null) {
                fallback.add(new AbstractMap.SimpleEntry<>(ma, fallbackDto));
            }
        }
        return fallback;
    }

    public void notifyPartnerAdRemovalRequested(Client partner, Ad ad, Monitor monitor, String partnerMessage) {
        if (partner == null || ad == null) {
            return;
        }
        String screenSummary = formatMonitorLine(monitor);
        String trimmedMessage = partnerMessage != null ? partnerMessage.trim() : "";

        Map<String, String> adminParams = new HashMap<>();
        adminParams.put("partnerName", partner.getBusinessName());
        adminParams.put("adName", ad.getName());
        adminParams.put("screenSummary", screenSummary);
        adminParams.put("message", trimmedMessage);
        adminParams.put("link", frontBaseUrl + "/admin/ad-requests");
        notifyAdmins(NotificationReference.PARTNER_AD_REMOVAL_REQUESTED, adminParams);

        Map<String, String> partnerParams = new HashMap<>();
        partnerParams.put("name", partner.getBusinessName());
        partnerParams.put("adName", ad.getName());
        partnerParams.put("link", frontBaseUrl + "/client/screens");
        notificationService.save(
                NotificationReference.PARTNER_AD_REMOVAL_REQUEST_CONFIRMED, partner, partnerParams, true);
    }
}
