package com.telas.services;

import com.telas.enums.NotificationReference;

import java.util.Map;

public interface DeveloperNotificationService {
    void notifyDevelopers(NotificationReference reference, Map<String, String> params);
}

