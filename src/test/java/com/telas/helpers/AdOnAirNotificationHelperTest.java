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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdOnAirNotificationHelperTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private AdminAdsNotificationService adminAdsNotificationService;
    @Mock
    private AdRepository adRepository;
    @Mock
    private ClientPortalLinkResolver clientPortalLinkResolver;

    @InjectMocks
    private AdOnAirNotificationHelper helper;

    @Test
    void notifyOnAirForNewMonitorAds_shouldDeduplicateByTimestampMarker() {
        Client client = new Client();
        client.setId(UUID.randomUUID());
        client.setBusinessName("ACME");

        Ad ad = new Ad();
        ad.setId(UUID.randomUUID());
        ad.setClient(client);
        ad.setName("ad.png");
        ad.setValidation(AdValidationType.APPROVED);
        ad.setOnAirNotifiedAt(Instant.now());

        MonitorAd ma = new MonitorAd();
        ma.setAd(ad);

        Monitor monitor = new Monitor();
        var box = new com.telas.entities.Box();
        box.setActive(true);
        monitor.setBox(box);

        helper.notifyOnAirForNewMonitorAds(List.of(ma), monitor);

        verify(notificationService, never()).save(eq(NotificationReference.CLIENT_AD_ON_AIR), any(), any(), anyBoolean());
        verify(adminAdsNotificationService, never()).notifyAdmins(any(), any(), anyBoolean());
        verify(adRepository, never()).save(any(Ad.class));
    }

    @Test
    void notifyOnAirForNewMonitorAds_whenEligible_shouldNotifyClientAndAdmins() {
        Client client = new Client();
        client.setId(UUID.randomUUID());
        client.setBusinessName("ACME");

        Ad ad = new Ad();
        ad.setId(UUID.randomUUID());
        ad.setClient(client);
        ad.setName("ad.png");
        ad.setValidation(AdValidationType.APPROVED);
        ad.setOnAirNotifiedAt(null);

        MonitorAd ma = new MonitorAd();
        ma.setAd(ad);

        Monitor monitor = new Monitor();
        var box = new com.telas.entities.Box();
        box.setActive(true);
        var addr = new com.telas.entities.BoxAddress();
        addr.setIp("10.0.0.1");
        box.setBoxAddress(addr);
        monitor.setBox(box);

        when(clientPortalLinkResolver.clientAdsTabLink(client)).thenReturn("https://front.test/client/my-telas?tab=ads");
        when(clientPortalLinkResolver.partnerFlag(client)).thenReturn("false");
        when(clientPortalLinkResolver.adminClientMessagesLink(client.getId()))
                .thenReturn("https://front.test/admin/clients/" + client.getId() + "/messages");
        when(adRepository.save(any(Ad.class))).thenAnswer(inv -> inv.getArgument(0));

        helper.notifyOnAirForNewMonitorAds(List.of(ma), monitor);

        verify(notificationService).save(eq(NotificationReference.CLIENT_AD_ON_AIR), eq(client), any(), eq(true));
        verify(adminAdsNotificationService).notifyAdmins(
                eq(NotificationReference.ADMIN_AD_ON_AIR),
                any(),
                eq(true));
        verify(adRepository).save(eq(ad));
    }

    @Test
    void notifyOnAirForNewMonitorAds_whenSendEmailFalse_skipsEmailFlags() {
        Client client = new Client();
        client.setId(UUID.randomUUID());
        client.setBusinessName("ACME");

        Ad ad = new Ad();
        ad.setId(UUID.randomUUID());
        ad.setClient(client);
        ad.setName("ad.png");
        ad.setValidation(AdValidationType.APPROVED);
        ad.setOnAirNotifiedAt(null);

        MonitorAd ma = new MonitorAd();
        ma.setAd(ad);

        Monitor monitor = new Monitor();
        var box = new com.telas.entities.Box();
        box.setActive(true);
        var addr = new com.telas.entities.BoxAddress();
        addr.setIp("10.0.0.1");
        box.setBoxAddress(addr);
        monitor.setBox(box);

        when(clientPortalLinkResolver.clientAdsTabLink(client)).thenReturn("https://front.test/client/my-telas?tab=ads");
        when(clientPortalLinkResolver.partnerFlag(client)).thenReturn("false");
        when(clientPortalLinkResolver.adminClientMessagesLink(client.getId()))
                .thenReturn("https://front.test/admin/clients/" + client.getId() + "/messages");
        when(adRepository.save(any(Ad.class))).thenAnswer(inv -> inv.getArgument(0));

        helper.notifyOnAirForNewMonitorAds(List.of(ma), monitor, false);

        verify(notificationService).save(eq(NotificationReference.CLIENT_AD_ON_AIR), eq(client), any(), eq(false));
        verify(adminAdsNotificationService).notifyAdmins(
                eq(NotificationReference.ADMIN_AD_ON_AIR),
                any(),
                eq(false));
    }
}
