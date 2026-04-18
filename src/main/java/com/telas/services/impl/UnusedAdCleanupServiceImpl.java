package com.telas.services.impl;

import com.telas.entities.Ad;
import com.telas.repositories.AdRepository;
import com.telas.scheduler.SchedulerJobRunContext;
import com.telas.services.ApplicationLogService;
import com.telas.services.UnusedAdCleanupService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UnusedAdCleanupServiceImpl implements UnusedAdCleanupService {

    private static final Logger log = LoggerFactory.getLogger(UnusedAdCleanupServiceImpl.class);
    private static final int SUMMARY_MAX_AD_ENTRIES = 80;

    private final AdRepository adRepository;
    private final ApplicationLogService applicationLogService;
    private final UnusedSingleAdDeletionService unusedSingleAdDeletionService;
    private final SchedulerJobRunContext schedulerJobRunContext;

    @Value("${cleanup.ads.retention.days:30}")
    private int globalRetentionDays;

    @Override
    public int deleteEligibleUnusedAds() {
        List<UUID> ids = adRepository.findIdsEligibleForRetentionCleanup(globalRetentionDays);
        if (ids.isEmpty()) {
            schedulerJobRunContext.put("adsDeletedCount", 0);
            schedulerJobRunContext.put("deletedAds", List.of());
            return 0;
        }
        Map<UUID, String> idToName =
                adRepository.findAllById(ids).stream().collect(Collectors.toMap(Ad::getId, Ad::getName));
        int removed = 0;
        int failures = 0;
        List<Map<String, String>> deletedAds = new ArrayList<>();
        boolean truncated = false;
        for (UUID id : ids) {
            try {
                unusedSingleAdDeletionService.deleteAdInNewTransaction(id);
                removed++;
                if (deletedAds.size() < SUMMARY_MAX_AD_ENTRIES) {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("adId", id.toString());
                    row.put("adName", idToName.getOrDefault(id, ""));
                    deletedAds.add(row);
                } else {
                    truncated = true;
                }
            } catch (Exception ex) {
                failures++;
                log.warn("Unused ad cleanup failed for ad id={}: {}", id, ex.getMessage());
            }
        }
        schedulerJobRunContext.put("adsDeletedCount", removed);
        schedulerJobRunContext.put("adsDeleteFailures", failures);
        schedulerJobRunContext.put("deletedAds", deletedAds);
        if (truncated) {
            schedulerJobRunContext.put("deletedAdsTruncated", true);
        }
        if (removed > 0) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("removedCount", removed);
            meta.put("deletedAds", deletedAds);
            if (truncated) {
                meta.put("deletedAdsTruncated", true);
            }
            if (failures > 0) {
                meta.put("deleteFailures", failures);
            }
            applicationLogService.persistSystemLog(
                    "INFO",
                    buildRetentionRemovalMessage(removed, deletedAds, truncated, failures),
                    "WORKER",
                    meta);
        }
        return removed;
    }

    private static String buildRetentionRemovalMessage(
            int removed,
            List<Map<String, String>> deletedAds,
            boolean truncated,
            int failures) {
        StringBuilder sb = new StringBuilder();
        sb.append("Unused ad retention removed ")
                .append(removed)
                .append(" ad(s).");
        if (!deletedAds.isEmpty()) {
            sb.append(" Removed: ");
            for (int i = 0; i < deletedAds.size(); i++) {
                if (i > 0) {
                    sb.append("; ");
                }
                Map<String, String> row = deletedAds.get(i);
                String name = row.getOrDefault("adName", "").trim();
                String aid = row.getOrDefault("adId", "");
                if (!name.isEmpty()) {
                    sb.append('"').append(name).append("\" (").append(aid).append(')');
                } else {
                    sb.append(aid);
                }
            }
            if (truncated) {
                int more = removed - deletedAds.size();
                if (more > 0) {
                    sb.append(" … (").append(more).append(" more not listed in message)");
                }
            }
        }
        if (failures > 0) {
            sb.append(" Failures: ").append(failures).append('.');
        }
        String out = sb.toString();
        return out.length() > 3900 ? out.substring(0, 3897) + "..." : out;
    }
}
