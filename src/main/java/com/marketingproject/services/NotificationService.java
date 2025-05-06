package com.marketingproject.services;

import com.marketingproject.entities.Client;
import com.marketingproject.enums.NotificationReference;

import java.util.Map;

public interface NotificationService {
    void save(NotificationReference notificationReference, Client client, Map<String, String> params);

//    NotificationResponseDto findById(UUID id);

//    List<NotificationResponseDto> listClientNotifications(NotificationRequestDto request);

    void notify(NotificationReference notificationReference, Client client, Map<String, String> params);
}
