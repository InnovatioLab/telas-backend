package com.telas.services.impl;

import com.telas.monitoring.entities.SmartPlugEntity;
import com.telas.monitoring.repositories.SmartPlugEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SmartPlugIpDiscoveryPersistence {

    private final SmartPlugEntityRepository smartPlugEntityRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateLastSeenIp(UUID plugId, String ip) {
        SmartPlugEntity p = smartPlugEntityRepository.findById(plugId).orElseThrow();
        p.setLastSeenIp(ip);
        p.setUpdatedAt(Instant.now());
        smartPlugEntityRepository.save(p);
    }
}
