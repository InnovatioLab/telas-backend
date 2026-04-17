package com.telas.services;

import com.telas.enums.AdminEmailAlertCategory;
import com.telas.enums.NotificationReference;

import java.util.Map;

public interface AdminMonitoringNotificationService {

    void notifyAdmins(NotificationReference reference, Map<String, String> params, AdminEmailAlertCategory emailCategory);
}
