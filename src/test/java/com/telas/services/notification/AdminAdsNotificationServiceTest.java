package com.telas.services.notification;

import com.telas.entities.Client;
import com.telas.enums.AdminEmailAlertCategory;
import com.telas.enums.NotificationReference;
import com.telas.enums.Permission;
import com.telas.repositories.ClientRepository;
import com.telas.services.AdminEmailAlertPreferenceService;
import com.telas.services.NotificationService;
import com.telas.services.PermissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAdsNotificationServiceTest {

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PermissionService permissionService;
    @Mock
    private AdminEmailAlertPreferenceService adminEmailAlertPreferenceService;

    @InjectMocks
    private AdminAdsNotificationService service;

    @Test
    void notifyAdmins_whenRecipientCannotManageAds_skipsNotification() {
        Client recipient = new Client();
        recipient.setId(UUID.randomUUID());
        when(clientRepository.findAllAdminsAndDevelopers()).thenReturn(List.of(recipient));
        when(permissionService.hasPermission(recipient, Permission.ADMIN_ADS_MANAGE)).thenReturn(false);

        service.notifyAdmins(NotificationReference.ADMIN_AD_ON_AIR, Map.of("adName", "ad.png"));

        verify(notificationService, never()).save(any(), any(), any(), anyBoolean());
    }

    @Test
    void notifyAdmins_whenEligibleAdminAndEmailEnabled_sendsWithPreference() {
        Client recipient = new Client();
        recipient.setId(UUID.randomUUID());
        when(clientRepository.findAllAdminsAndDevelopers()).thenReturn(List.of(recipient));
        when(permissionService.hasPermission(recipient, Permission.ADMIN_ADS_MANAGE)).thenReturn(true);
        when(adminEmailAlertPreferenceService.wantsEmail(recipient.getId(), AdminEmailAlertCategory.ADS_MANAGEMENT))
                .thenReturn(true);

        service.notifyAdmins(NotificationReference.ADMIN_AD_ON_AIR, Map.of("adName", "ad.png"));

        verify(notificationService).save(
                eq(NotificationReference.ADMIN_AD_ON_AIR),
                eq(recipient),
                any(),
                eq(true));
    }

    @Test
    void notifyAdmins_whenSendEmailFalse_skipsEmailFlag() {
        Client recipient = new Client();
        recipient.setId(UUID.randomUUID());
        when(clientRepository.findAllAdminsAndDevelopers()).thenReturn(List.of(recipient));
        when(permissionService.hasPermission(recipient, Permission.ADMIN_ADS_MANAGE)).thenReturn(true);

        service.notifyAdmins(NotificationReference.ADMIN_AD_ON_AIR, Map.of("adName", "ad.png"), false);

        verify(notificationService).save(
                eq(NotificationReference.ADMIN_AD_ON_AIR),
                eq(recipient),
                any(),
                eq(false));
        verify(adminEmailAlertPreferenceService, never()).wantsEmail(any(), any());
    }
}
