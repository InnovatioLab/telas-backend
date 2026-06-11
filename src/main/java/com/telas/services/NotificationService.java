package com.telas.services;

import com.telas.dtos.response.NotificationResponseDto;
import com.telas.entities.Client;
import com.telas.entities.Notification;
import com.telas.enums.NotificationReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface NotificationService {
    void save(NotificationReference notificationReference, Client client, Map<String, String> params, boolean sendEmail);

    NotificationResponseDto findById(UUID id);

    Page<NotificationResponseDto> listClientNotifications(List<UUID> ids, Specification<Notification> spec, Pageable pageable);

    void markAllAsRead();
}
