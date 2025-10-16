package com.telas.services.impl;


import com.telas.entities.WebhookEvent;
import com.telas.repositories.WebhookEventRepository;
import com.telas.services.EventService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final Logger log = LoggerFactory.getLogger(EventServiceImpl.class);
    private final WebhookEventRepository repository;


    @Override
    public void save(String eventId, String eventType) {
        if (!checkIfExists(eventId)) {
            repository.save(new WebhookEvent(eventId, eventType));
            log.info("[WORKER]: Event with ID: {} saved.", eventId);
        }
    }

    @Override
    public boolean checkIfExists(String eventId) {
        return repository.existsById(eventId);
    }
}
