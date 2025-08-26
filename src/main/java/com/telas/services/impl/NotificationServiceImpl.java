package com.telas.services.impl;

import com.telas.dtos.EmailDataDto;
import com.telas.dtos.request.NotificationRequestDto;
import com.telas.dtos.response.NotificationResponseDto;
import com.telas.entities.Client;
import com.telas.entities.Notification;
import com.telas.enums.NotificationReference;
import com.telas.infra.exceptions.ForbiddenException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.NotificationRepository;
import com.telas.services.EmailService;
import com.telas.services.NotificationService;
import com.telas.shared.constants.MessageCommonsConstants;
import com.telas.shared.constants.valitation.AuthValidationMessageConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
  private final NotificationRepository repository;
  private final AuthenticatedUserService authenticatedUserService;
  private final EmailService emailService;

  @Override
  @Transactional
  public void save(NotificationReference notificationReference, Client client, Map<String, String> params, boolean sendEmail) {
    Notification notification = repository.save(new Notification(notificationReference, client, params));
    if (sendEmail) {
      notify(notification, params);
    }
  }

  @Override
  public NotificationResponseDto findById(UUID id) {
    Client client = authenticatedUserService.getLoggedUser().client();
    Notification notification = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(MessageCommonsConstants.NOTIFICATION_NOT_FOUND));

    if (!client.getId().equals(notification.getClient().getId())) {
      throw new ForbiddenException(AuthValidationMessageConstants.ERROR_NO_PERMISSION);
    }

    notification.setVisualized(true);
    return new NotificationResponseDto(repository.save(notification));
  }

  @Override
  @Transactional
  public List<NotificationResponseDto> listClientNotifications(NotificationRequestDto request) {
    Client client = authenticatedUserService.getLoggedUser().client();
    List<Notification> notifications = repository.findAllByClientIdOrderByCreatedAtDesc(client.getId());

    if (request.getIds() != null && !request.getIds().isEmpty()) {
      notifications.stream()
              .filter(notification -> request.getIds().contains(notification.getId()))
              .forEach(notification -> {
                notification.setVisualized(true);
                repository.save(notification);
              });
      return Collections.emptyList();
    }

    return notifications.stream().map(NotificationResponseDto::new).toList();
  }

  private void notify(Notification notification, Map<String, String> params) {
    EmailDataDto emailData = notification.getReference().getEmailData(params);
    emailData.setEmail(notification.getClient().getContact().getEmail());
    emailService.send(emailData);
  }
}
