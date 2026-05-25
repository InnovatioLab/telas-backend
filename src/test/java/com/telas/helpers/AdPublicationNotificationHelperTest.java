package com.telas.helpers;

import com.telas.dtos.request.UpdateBoxMonitorsAdRequestDto;
import com.telas.entities.Ad;
import com.telas.entities.Client;
import com.telas.entities.Monitor;
import com.telas.entities.MonitorAd;
import com.telas.enums.AdValidationType;
import com.telas.enums.NotificationReference;
import com.telas.enums.Role;
import com.telas.services.NotificationService;
import com.telas.services.notification.AdminAdsNotificationService;
import com.telas.shared.utils.ClientPortalLinkResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdPublicationNotificationHelperTest {

    @Mock
    private BoxAdPushNotificationHelper boxAdPushNotificationHelper;
    @Mock
    private AdOnAirNotificationHelper adOnAirNotificationHelper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AdminAdsNotificationService adminAdsNotificationService;
    @Mock
    private ClientPortalLinkResolver clientPortalLinkResolver;
    @Mock
    private MonitorSummaryFormatter monitorSummaryFormatter;

    @InjectMocks
    private AdPublicationNotificationHelper helper;

    @Test
    void notifyAfterPlaylistUpdate_withSuccessfulSync_shouldNotifyDeployAndOnAir() {
        Client client = new Client();
        client.setId(UUID.randomUUID());
        client.setBusinessName("Partner Co");

        Ad ad = new Ad();
        ad.setId(UUID.randomUUID());
        ad.setClient(client);
        ad.setName("ad-1.jpg");
        ad.setValidation(AdValidationType.APPROVED);

        MonitorAd monitorAd = new MonitorAd();
        monitorAd.setAd(ad);

        Monitor monitor = new Monitor();
        monitor.setId(UUID.randomUUID());
        monitor.getMonitorAds().add(monitorAd);

        UpdateBoxMonitorsAdRequestDto dto = new UpdateBoxMonitorsAdRequestDto();
        dto.setFileName("ad-1.jpg");
        dto.setBaseUrl("http://192.168.0.1:8081/");

        helper.notifyAfterPlaylistUpdate(
                monitor,
                List.of(monitorAd),
                Set.of("http://192.168.0.1:8081/"),
                List.of(dto));

        verify(boxAdPushNotificationHelper).notifyAfterSuccessfulPush(any(), eq(Set.of("http://192.168.0.1:8081/")));
        verify(adOnAirNotificationHelper).notifyOnAirForNewMonitorAds(
                List.of(monitorAd), monitor, true);
    }

    @Test
    void notifyAfterPlaylistUpdate_withoutBoxSync_shouldNotifyPendingPlaylist() {
        Client client = new Client();
        client.setId(UUID.randomUUID());
        client.setBusinessName("Partner Co");
        client.setRole(Role.PARTNER);

        Ad ad = new Ad();
        ad.setId(UUID.randomUUID());
        ad.setClient(client);
        ad.setName("ad-2.jpg");
        ad.setValidation(AdValidationType.APPROVED);

        MonitorAd monitorAd = new MonitorAd();
        monitorAd.setAd(ad);

        Monitor monitor = new Monitor();
        monitor.setId(UUID.randomUUID());

        when(monitorSummaryFormatter.formatMonitorLine(monitor)).thenReturn("");
        when(clientPortalLinkResolver.clientAdsTabLink(client)).thenReturn("https://front.test/client/screens");
        when(clientPortalLinkResolver.partnerFlag(client)).thenReturn("true");
        when(clientPortalLinkResolver.adminClientMessagesLink(client.getId()))
                .thenReturn("https://front.test/admin/clients/" + client.getId() + "/messages");

        helper.notifyAfterPlaylistUpdate(monitor, List.of(monitorAd), Set.of(), List.of());

        verify(notificationService).save(
                eq(NotificationReference.AD_ADDED_TO_PLAYLIST_PENDING_SYNC),
                eq(client),
                any(),
                eq(true));
        verify(adminAdsNotificationService).notifyAdmins(
                eq(NotificationReference.ADMIN_AD_ADDED_TO_PLAYLIST_PENDING_SYNC),
                any());
        verify(adOnAirNotificationHelper).notifyOnAirForNewMonitorAds(
                List.of(monitorAd), monitor, false);
    }
}
