package com.telas.services.impl;

import com.telas.dtos.EmailDataDto;
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
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository repository;
    private final AuthenticatedUserService authenticatedUserService;
    private final EmailService emailService;

    @Value("${front.base.url}")
    private String frontBaseUrl;

    @Override
    @Transactional
    public void save(NotificationReference notificationReference, Client client, Map<String, String> params, boolean sendEmail) {
        Notification notification = repository.save(new Notification(notificationReference, client, params));
        if (!sendEmail) {
            return;
        }
        UUID notificationId = notification.getId();
        Map<String, String> paramsCopy = params == null ? Map.of() : new HashMap<>(params);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    loadAndNotify(notificationId, paramsCopy);
                }
            });
        } else {
            loadAndNotify(notificationId, paramsCopy);
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
    public List<NotificationResponseDto> listClientNotifications(List<UUID> ids) {
        Client client = authenticatedUserService.getLoggedUser().client();
        List<Notification> notifications = repository.findAllByClientIdOrderByCreatedAtDesc(client.getId());

        if (Objects.nonNull(ids) && !ValidateDataUtils.isNullOrEmpty(ids)) {
            notifications.stream()
                    .filter(notification -> ids.contains(notification.getId()))
                    .forEach(notification -> {
                        notification.setVisualized(true);
                        repository.save(notification);
                    });
        }

        return notifications.stream().map(NotificationResponseDto::new).toList();
    }

    private void loadAndNotify(UUID notificationId, Map<String, String> params) {
        repository.findWithClientAndContactForEmail(notificationId).ifPresentOrElse(n -> notify(n, params), () ->
                LOGGER.warn("notification.not_found.after_commit id={}", notificationId)
        );
    }

    private void notify(Notification notification, Map<String, String> params) {
        EmailDataDto emailData = notification.getReference().getEmailData(params);
        if (emailData == null) {
            return;
        }

        Map<String, String> emailParams = emailData.getParams();
        if (emailParams != null) {
            String link = emailParams.get("link");
            if (!ValidateDataUtils.isNullOrEmptyString(link) && !link.startsWith(frontBaseUrl)) {
                String base = frontBaseUrl.endsWith("/") ? frontBaseUrl.substring(0, frontBaseUrl.length() - 1) : frontBaseUrl;
                String newLink = link.startsWith("/") ? base + link : base + "/" + link;
                emailParams.put("link", newLink);
            }
        }

        emailData.setEmail(notification.getClient().getContact().getEmail());
        if (emailData.getParams() != null) {
            emailData.getParams().put("clientId", notification.getClient().getId().toString());
        }
        try {
            emailService.send(emailData);
        } catch (RuntimeException ex) {
            LOGGER.error(
                    "notification.email.send.failed reference={} clientId={} template={}",
                    notification.getReference(),
                    notification.getClient().getId(),
                    emailData.getTemplate(),
                    ex
            );
        }
    }


}
