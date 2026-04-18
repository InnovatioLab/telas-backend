package com.telas.services.impl;

import com.telas.entities.Client;
import com.telas.enums.AdminEmailAlertCategory;
import com.telas.enums.NotificationReference;
import com.telas.repositories.ClientRepository;
import com.telas.services.AdminEmailAlertPreferenceService;
import com.telas.services.AdminMonitoringNotificationService;
import com.telas.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminMonitoringNotificationServiceImpl implements AdminMonitoringNotificationService {

    private final ClientRepository clientRepository;
    private final NotificationService notificationService;
    private final AdminEmailAlertPreferenceService adminEmailAlertPreferenceService;

    @Override
    public void notifyAdmins(
            NotificationReference reference, Map<String, String> params, AdminEmailAlertCategory emailCategory) {
        for (Client recipient : clientRepository.findAllAdminsAndDevelopers()) {
            boolean sendEmail = adminEmailAlertPreferenceService.wantsEmail(recipient.getId(), emailCategory);
            notificationService.save(reference, recipient, params, sendEmail);
        }
    }
}
