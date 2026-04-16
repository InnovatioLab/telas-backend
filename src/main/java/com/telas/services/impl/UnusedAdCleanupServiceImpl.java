package com.telas.services.impl;

import com.telas.repositories.AdRepository;
import com.telas.services.ApplicationLogService;
import com.telas.services.UnusedAdCleanupService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UnusedAdCleanupServiceImpl implements UnusedAdCleanupService {

    private static final Logger log = LoggerFactory.getLogger(UnusedAdCleanupServiceImpl.class);

    private final AdRepository adRepository;
    private final ApplicationLogService applicationLogService;
    private final UnusedSingleAdDeletionService unusedSingleAdDeletionService;

    @Value("${cleanup.ads.retention.days:30}")
    private int globalRetentionDays;

    @Override
    public int deleteEligibleUnusedAds() {
        List<UUID> ids = adRepository.findIdsEligibleForRetentionCleanup(globalRetentionDays);
        if (ids.isEmpty()) {
            return 0;
        }
        int removed = 0;
        for (UUID id : ids) {
            try {
                unusedSingleAdDeletionService.deleteAdInNewTransaction(id);
                removed++;
            } catch (Exception ex) {
                log.warn("Unused ad cleanup failed for ad id={}: {}", id, ex.getMessage());
            }
        }
        if (removed > 0) {
            applicationLogService.persistSystemLog(
                    "INFO",
                    "Unused ad retention removed " + removed + " ad(s).",
                    "WORKER",
                    new HashMap<>());
        }
        return removed;
    }
}
