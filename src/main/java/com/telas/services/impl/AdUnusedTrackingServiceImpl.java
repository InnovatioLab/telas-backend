package com.telas.services.impl;

import com.telas.entities.Ad;
import com.telas.enums.AdValidationType;
import com.telas.repositories.AdRepository;
import com.telas.repositories.MonitorAdRepository;
import com.telas.services.AdUnusedTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdUnusedTrackingServiceImpl implements AdUnusedTrackingService {

    private final AdRepository adRepository;
    private final MonitorAdRepository monitorAdRepository;

    @Override
    @Transactional
    public void syncUnusedStateForAdIds(Collection<UUID> adIds) {
        if (adIds == null || adIds.isEmpty()) {
            return;
        }
        for (UUID id : adIds) {
            if (id == null) {
                continue;
            }
            adRepository.findById(id).ifPresent(this::syncOne);
        }
    }

    private void syncOne(Ad ad) {
        if (ad.getValidation() != AdValidationType.APPROVED) {
            ad.setUnusedSince(null);
            adRepository.save(ad);
            return;
        }
        long onMonitors = monitorAdRepository.countByAdId(ad.getId());
        if (onMonitors > 0) {
            ad.setUnusedSince(null);
        } else if (ad.getUnusedSince() == null) {
            ad.setUnusedSince(Instant.now());
        }
        adRepository.save(ad);
    }
}
