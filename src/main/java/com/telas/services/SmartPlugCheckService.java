package com.telas.services;

import com.telas.monitoring.crypto.AesTextEncryptionService;
import com.telas.monitoring.entities.CheckRunEntity;
import com.telas.monitoring.entities.SmartPlugAccountEntity;
import com.telas.monitoring.entities.SmartPlugEntity;
import com.telas.monitoring.plug.PlugReading;
import com.telas.monitoring.plug.SmartPlugClient;
import com.telas.monitoring.plug.SmartPlugCredentials;
import com.telas.monitoring.repositories.SmartPlugAccountEntityRepository;
import com.telas.monitoring.repositories.BoxHeartbeatEntityRepository;
import com.telas.monitoring.repositories.CheckRunEntityRepository;
import com.telas.monitoring.repositories.SmartPlugEntityRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SmartPlugCheckService {

    private static final Logger log = LoggerFactory.getLogger(SmartPlugCheckService.class);

    private final SmartPlugEntityRepository smartPlugEntityRepository;
    private final SmartPlugClient smartPlugClient;
    private final AesTextEncryptionService encryptionService;
    private final SmartPlugAccountEntityRepository smartPlugAccountEntityRepository;
    private final BoxHeartbeatEntityRepository boxHeartbeatEntityRepository;
    private final CheckRunEntityRepository checkRunEntityRepository;
    private final SmartPlugRulesService smartPlugRulesService;

    @Value("${monitoring.kasa.raise-incidents:true}")
    private boolean raiseIncidents;

    @Value("${monitoring.heartbeat.stale-seconds:180}")
    private long staleSeconds;

    private static final int KASA_SUMMARY_MAX_FAILED_IDS = 50;

    @Transactional
    public Map<String, Object> runAllChecks() {
        List<SmartPlugEntity> plugs = smartPlugEntityRepository.findAllEnabledForChecks();
        int failures = 0;
        List<String> failedPlugIds = new ArrayList<>();
        for (SmartPlugEntity plug : plugs) {
            try {
                runSingle(plug);
            } catch (Exception e) {
                failures++;
                log.warn("Smart plug check failed plugId={}", plug.getId(), e);
                if (failedPlugIds.size() < KASA_SUMMARY_MAX_FAILED_IDS) {
                    failedPlugIds.add(plug.getId().toString());
                }
            }
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("kasaPlugsChecked", plugs.size());
        summary.put("kasaFailures", failures);
        summary.put("kasaFailedPlugIds", failedPlugIds);
        if (failures > failedPlugIds.size()) {
            summary.put("kasaFailedIdsTruncated", true);
        }
        return summary;
    }

    private void runSingle(SmartPlugEntity plug) {
        SmartPlugCredentials credentials = resolveCredentials(plug);
        PlugReading reading = smartPlugClient.read(plug, credentials);
        persistCheckRun(plug, reading);
        if (!raiseIncidents) {
            return;
        }
        boolean heartbeatStale = isHeartbeatStale(plug);
        smartPlugRulesService.evaluate(plug, reading, heartbeatStale);
    }

    private SmartPlugCredentials resolveCredentials(SmartPlugEntity plug) {
        String username = resolveUsername(plug);
        String password = resolvePassword(plug);
        if ((username == null || username.isBlank()) && (password == null || password.isBlank())) {
            return null;
        }
        return new SmartPlugCredentials(username, password);
    }

    private String resolveUsername(SmartPlugEntity plug) {
        SmartPlugAccountEntity a = resolveAccount(plug);
        if (a != null && a.getAccountEmail() != null && !a.getAccountEmail().isBlank()) {
            return a.getAccountEmail();
        }
        return plug.getAccountEmail();
    }

    private String resolvePassword(SmartPlugEntity plug) {
        SmartPlugAccountEntity a = resolveAccount(plug);
        if (a != null) {
            return decryptOrNull(a.getPasswordCipher());
        }
        return decryptOrNull(plug.getPasswordCipher());
    }

    private SmartPlugAccountEntity resolveAccount(SmartPlugEntity plug) {
        if (plug.getSmartPlugAccount() != null) {
            SmartPlugAccountEntity a = plug.getSmartPlugAccount();
            return a.isEnabled() ? a : null;
        }
        UUID boxId = resolveHeartbeatBoxId(plug);
        if (boxId == null) {
            return null;
        }
        return smartPlugAccountEntityRepository
                .findByBox_IdAndVendorAndEnabledTrue(boxId, plug.getVendor())
                .orElse(null);
    }

    private String decryptOrNull(String cipher) {
        if (cipher == null || cipher.isBlank()) {
            return null;
        }
        if (!encryptionService.isConfigured()) {
            return null;
        }
        return encryptionService.decrypt(cipher);
    }

    private boolean isHeartbeatStale(SmartPlugEntity plug) {
        UUID boxId = resolveHeartbeatBoxId(plug);
        if (boxId == null) {
            return true;
        }
        Instant cutoff = Instant.now().minusSeconds(staleSeconds);
        return boxHeartbeatEntityRepository
                .findByBox_Id(boxId)
                .map(h -> h.getLastSeenAt().isBefore(cutoff))
                .orElse(true);
    }

    private static UUID resolveHeartbeatBoxId(SmartPlugEntity plug) {
        if (plug.getMonitor() != null && plug.getMonitor().getBox() != null) {
            return plug.getMonitor().getBox().getId();
        }
        if (plug.getBox() != null) {
            return plug.getBox().getId();
        }
        return null;
    }

    private void persistCheckRun(SmartPlugEntity plug, PlugReading reading) {
        CheckRunEntity run = new CheckRunEntity();
        run.setSmartPlug(plug);
        run.setStartedAt(Instant.now());
        run.setFinishedAt(Instant.now());
        run.setSuccess(reading.reachable());
        if (!reading.reachable()) {
            run.setErrorMessage(reading.errorCode());
        }
        Map<String, Object> meta = new HashMap<>();
        meta.put("relayOn", reading.relayOn());
        meta.put("powerWatts", reading.powerWatts());
        meta.put("voltageVolts", reading.voltageVolts());
        meta.put("currentAmperes", reading.currentAmperes());
        meta.put("confidence", "medium");
        run.setMetadataJson(meta);
        checkRunEntityRepository.save(run);
    }

    // incident/rules evaluation extracted to SmartPlugRulesService
}
