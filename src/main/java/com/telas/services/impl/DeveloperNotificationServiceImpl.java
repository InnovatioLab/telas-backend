package com.telas.services.impl;

import com.telas.entities.Client;
import com.telas.enums.NotificationReference;
import com.telas.repositories.ClientRepository;
import com.telas.services.DeveloperNotificationService;
import com.telas.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeveloperNotificationServiceImpl implements DeveloperNotificationService {
    private final ClientRepository clientRepository;
    private final NotificationService notificationService;

    @Override
    public void notifyDevelopers(NotificationReference reference, Map<String, String> params) {
        for (Client dev : clientRepository.findAllDevelopers()) {
            notificationService.save(reference, dev, params, false);
        }
    }
}

