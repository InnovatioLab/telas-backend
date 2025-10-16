package com.telas.services;

import org.springframework.transaction.annotation.Transactional;

public interface EventService {
    @Transactional
    void save(String eventId, String eventType);

    boolean checkIfExists(String eventId);
}

