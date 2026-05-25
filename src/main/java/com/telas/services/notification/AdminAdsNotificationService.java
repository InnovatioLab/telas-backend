package com.telas.services.notification;

import com.telas.entities.Client;
import com.telas.enums.AdminEmailAlertCategory;
import com.telas.enums.NotificationReference;
import com.telas.enums.Permission;
import com.telas.repositories.ClientRepository;
import com.telas.services.AdminEmailAlertPreferenceService;
import com.telas.services.NotificationService;
import com.telas.services.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminAdsNotificationService {

    private final ClientRepository clientRepository;
    private final NotificationService notificationService;
    private final PermissionService permissionService;
    private final AdminEmailAlertPreferenceService adminEmailAlertPreferenceService;

    public void notifyAdmins(NotificationReference reference, Map<String, String> params) {
        notifyAdmins(reference, params, true);
    }

    public void notifyAdmins(NotificationReference reference, Map<String, String> params, boolean sendEmailNotifications) {
        for (Client recipient : clientRepository.findAllAdminsAndDevelopers()) {
            if (!canManageAds(recipient)) {
                continue;
            }
            boolean sendEmail = sendEmailNotifications
                    && !recipient.isDeveloper()
                    && adminEmailAlertPreferenceService.wantsEmail(
                            recipient.getId(), AdminEmailAlertCategory.ADS_MANAGEMENT);
            notificationService.save(reference, recipient, new HashMap<>(params), sendEmail);
        }
    }

    private boolean canManageAds(Client recipient) {
        return recipient.isDeveloper()
                || permissionService.hasPermission(recipient, Permission.ADMIN_ADS_MANAGE);
    }
}
