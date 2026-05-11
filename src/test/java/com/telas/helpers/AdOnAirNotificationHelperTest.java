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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
    private ClientRepository clientRepository;
    @Mock
    private PermissionService permissionService;
    @Mock
    private AdminEmailAlertPreferenceService adminEmailAlertPreferenceService;
    @Mock
    private AdRepository adRepository;

    @InjectMocks
    private AdOnAirNotificationHelper helper;

    @Test
    void notifyOnAirForNewMonitorAds_shouldDeduplicateByTimestampMarker() {
        ReflectionTestUtils.setField(helper, "frontBaseUrl", "https://front.test");

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
        verify(notificationService, never()).save(eq(NotificationReference.ADMIN_AD_ON_AIR), any(), any(), anyBoolean());
        verify(adRepository, never()).save(any(Ad.class));
    }

    @Test
    void notifyOnAirForNewMonitorAds_whenEligible_shouldNotifyClientAndAdmins() {
        ReflectionTestUtils.setField(helper, "frontBaseUrl", "https://front.test");

        Client client = new Client();
        client.setId(UUID.randomUUID());
        client.setBusinessName("ACME");

        Client admin = new Client();
        admin.setId(UUID.randomUUID());

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

        when(clientRepository.findAllAdminsAndDevelopers()).thenReturn(List.of(admin));
        when(permissionService.hasPermission(admin, Permission.ADMIN_ADS_MANAGE)).thenReturn(true);
        when(adminEmailAlertPreferenceService.wantsEmail(admin.getId(), com.telas.enums.AdminEmailAlertCategory.ADS_MANAGEMENT))
                .thenReturn(true);
        when(adRepository.save(any(Ad.class))).thenAnswer(inv -> inv.getArgument(0));

        helper.notifyOnAirForNewMonitorAds(List.of(ma), monitor);

        verify(notificationService).save(eq(NotificationReference.CLIENT_AD_ON_AIR), eq(client), any(), eq(true));
        verify(notificationService).save(eq(NotificationReference.ADMIN_AD_ON_AIR), eq(admin), any(), eq(true));
        verify(adRepository).save(eq(ad));
    }

    @Test
    void notifyOnAirForNewMonitorAds_whenSendEmailFalse_skipsEmailFlags() {
        ReflectionTestUtils.setField(helper, "frontBaseUrl", "https://front.test");

        Client client = new Client();
        client.setId(UUID.randomUUID());
        client.setBusinessName("ACME");

        Client admin = new Client();
        admin.setId(UUID.randomUUID());

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

        when(clientRepository.findAllAdminsAndDevelopers()).thenReturn(List.of(admin));
        when(permissionService.hasPermission(admin, Permission.ADMIN_ADS_MANAGE)).thenReturn(true);
        when(adRepository.save(any(Ad.class))).thenAnswer(inv -> inv.getArgument(0));

        helper.notifyOnAirForNewMonitorAds(List.of(ma), monitor, false);

        verify(notificationService).save(eq(NotificationReference.CLIENT_AD_ON_AIR), eq(client), any(), eq(false));
        verify(notificationService).save(eq(NotificationReference.ADMIN_AD_ON_AIR), eq(admin), any(), eq(false));
    }
}

