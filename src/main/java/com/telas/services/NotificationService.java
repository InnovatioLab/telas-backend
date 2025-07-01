package com.telas.services;

import com.telas.dtos.request.NotificationRequestDto;
import com.telas.dtos.response.NotificationResponseDto;
import com.telas.entities.Client;
import com.telas.enums.NotificationReference;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface NotificationService {
  void save(NotificationReference notificationReference, Client client, Map<String, String> params, boolean sendEmail);

  NotificationResponseDto findById(UUID id);

  List<NotificationResponseDto> listClientNotifications(NotificationRequestDto request);
}
