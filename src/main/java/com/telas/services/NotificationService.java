package com.telas.services;

import com.telas.entities.Client;
import com.telas.enums.NotificationReference;

import java.util.Map;

public interface NotificationService {
    void save(NotificationReference notificationReference, Client client, Map<String, String> params);

//    NotificationResponseDto findById(UUID id);

//    List<NotificationResponseDto> listClientNotifications(NotificationRequestDto request);

    void notify(NotificationReference notificationReference, Client client, Map<String, String> params);
}
